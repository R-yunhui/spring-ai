package com.ral.young.advisor;

import cn.hutool.core.util.StrUtil;
import com.ral.young.memeory.RoundBasedChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author renyh
 * @description 自定义 CustomMessageChatMemoryAdvisor
 * @date 2025/7/22 9:58
 * @since 1.0.0
 */
@Slf4j
@Service
public class UranMessageChatMemoryAdvisor implements BaseChatMemoryAdvisor {

	public RoundBasedChatMemory chatMemory;

	private String defaultConversationId;

	private String defaultRoundId;

	private String defaultFollowUpQuestionId;

	private String defaultFollowUpQuestionReferenceId;

	private Boolean defaultFollowUpQuestion;

	private int order;

	private Scheduler scheduler;

	@Override
	public int getOrder() {
		return this.order;
	}

	@NotNull
	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	public String getRoundId(Map<String, Object> context, String defaultRoundId) {
		return context.containsKey(RoundBasedChatMemory.ROUND_ID) ? context.get(RoundBasedChatMemory.ROUND_ID).toString()
				: defaultRoundId;
	}

	public String getFollowUpQuestionId(Map<String, Object> context, String defaultFollowUpQuestionId) {
		return context.containsKey(RoundBasedChatMemory.FOLLOW_UP_QUESTION_ID) ? context.get(RoundBasedChatMemory.FOLLOW_UP_QUESTION_ID).toString()
				: defaultFollowUpQuestionId;
	}

	public Boolean getHasFollowUpQuestion(Map<String, Object> context, Boolean defaultFollowUpQuestion) {
		return context.containsKey(RoundBasedChatMemory.FOLLOW_UP_QUESTION) ? (Boolean) context.get(RoundBasedChatMemory.FOLLOW_UP_QUESTION)
				: defaultFollowUpQuestion;
	}

	public String getDefaultFollowUpQuestionReferenceId(Map<String, Object> context, String defaultFollowUpQuestionReferenceId) {
		return context.containsKey(RoundBasedChatMemory.FOLLOW_UP_QUESTION_REFERENCE_ID) ? context.get(RoundBasedChatMemory.FOLLOW_UP_QUESTION_REFERENCE_ID).toString()
				: defaultFollowUpQuestionReferenceId;
	}

	@NotNull
	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, @NotNull AdvisorChain advisorChain) {
		// 会话 id
		String conversationId = getConversationId(chatClientRequest.context(), this.defaultConversationId);
		// 轮次 id
		String roundId = getRoundId(chatClientRequest.context(), this.defaultRoundId);
		// 追问 id
		String followUpQuestionId = getFollowUpQuestionId(chatClientRequest.context(), this.defaultFollowUpQuestionId);
		// 追问依赖的轮次 id
		String followUpQuestionReferenceId = getDefaultFollowUpQuestionReferenceId(chatClientRequest.context(), this.defaultFollowUpQuestionReferenceId);
		// 是否有追问
		Boolean followUpQuestion = getHasFollowUpQuestion(chatClientRequest.context(), this.defaultFollowUpQuestion);

		/*
		 * 1.多轮对话，不存在追问，则直接获取所有对话轮次的上下文
		 * 2.多伦对话，存在追问，
		 * 		2.1.如果不存在追问的id，则需要获取对应对话轮次的上下文，后续追问的上下文要继续添加到轮次的子上下文中，但是后续获取会话整体上下文的时候进行过滤
		 * 		2.2.如果存在追问的id，则获取对应完整的追问上下文（其中包含了它依赖轮次上下文）
		 */
		// 1. Retrieve the chat memory for the current conversation.
		List<Message> memoryMessages = getMemoryMessages(followUpQuestion, conversationId, followUpQuestionId, followUpQuestionReferenceId);

		// 2. Advise the request messages list.
		List<Message> processedMessages = new ArrayList<>(memoryMessages);
		processedMessages.addAll(chatClientRequest.prompt().getInstructions());

		// 3. Create a new request with the advised messages.
		ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
				.prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
				.build();

		boolean hasSystemMessage = memoryMessages.stream()
				.anyMatch(message -> message.getMessageType().equals(MessageType.SYSTEM));
		if (!hasSystemMessage) {
			log.info("会话:{}, 对话轮次:{} 添加对应的系统上下文消息", conversationId, roundId);
			this.chatMemory.addToRound(conversationId, roundId, processedChatClientRequest.prompt()
					.getSystemMessage());
		}

		// 4. Add the new user message to the conversation memory.
		UserMessage userMessage = processedChatClientRequest.prompt().getUserMessage();
		if (Boolean.TRUE.equals(followUpQuestion)) {
			// 将此次的上下文加入到 followUpQuestionId 中追问的轮次中
			log.info("会话:{} 依赖的对话轮次:{} 当前追问轮次:{} 添加追问上下文", conversationId, followUpQuestionReferenceId, followUpQuestionId);
			this.chatMemory.addToRound(conversationId, followUpQuestionId, userMessage);

			// 添加到 context 中，后续存储或者获取会话整体上下文时使用
			ChatOptions options = chatClientRequest.prompt().getOptions();
			if (options instanceof ToolCallingChatOptions toolCallingChatOptions) {
				// 添加到 toolContext里面，后面进行 tool 调用需要获取到这个来存放 tool 的上下文
				// toolCallingChatOptions.getToolContext() 是一个不可变的集合，需要重新创建一个 HashMap 来进行存储，在回填回去
				Map<String, Object> map = new HashMap<>(toolCallingChatOptions.getToolContext());
				map.put(RoundBasedChatMemory.FOLLOW_UP_QUESTION_ID, followUpQuestionId);
				toolCallingChatOptions.setToolContext(map);
				log.info("会话:{}, 依赖的对话轮次:{} 所属追问轮次:{} 添加到 tool context 中", conversationId, roundId, followUpQuestionId);
			}
		} else {
			log.info("会话:{}, 对话轮次:{} 不存在追问的情况，直接添加到对话轮次中", conversationId, roundId);
			this.chatMemory.addToRound(conversationId, roundId, userMessage);
		}
		return processedChatClientRequest;
	}

	/**
	 * 获取对话上下文
	 *
	 * @param followUpQuestion            是否有追问
	 * @param conversationId              会话 id
	 * @param followUpQuestionId          追问 id
	 * @param followUpQuestionReferenceId 追问依赖的轮次 id
	 * @return 对话上下文
	 */
	private @NotNull List<Message> getMemoryMessages(Boolean followUpQuestion, String conversationId,
													 String followUpQuestionId, String followUpQuestionReferenceId) {
		// 两种情况
		// 1. 追问，则需要获取对应对话轮次的上下文
		// 2. 追问，继续在追问之后问答，则需要获取整个追问对话的轮次上下文
		if (Boolean.TRUE.equals(followUpQuestion)) {
			log.info("开启了追问, 会话:{}, 对话轮次:{} 获取整个追问轮次:{} 上下文", conversationId, followUpQuestionReferenceId, followUpQuestionId);
			return this.chatMemory.getFollowupContext(conversationId, followUpQuestionId);
		} else {
			log.info("会话:{}, 获取完整的会话上下文", conversationId);
			return this.chatMemory.get(conversationId);
		}
	}

	@NotNull
	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, @NotNull AdvisorChain advisorChain) {
		List<Message> assistantMessages = new ArrayList<>();
		if (chatClientResponse.chatResponse() != null) {
			assistantMessages = chatClientResponse.chatResponse()
					.getResults()
					.stream()
					.map(g -> (Message) g.getOutput())
					.toList();
		}

		String conversationId = getConversationId(chatClientResponse.context(), this.defaultConversationId);
		String roundId = getRoundId(chatClientResponse.context(), this.defaultRoundId);
		String followUpQuestionId = getFollowUpQuestionId(chatClientResponse.context(), this.defaultFollowUpQuestionId);
		if (StrUtil.isNotBlank(followUpQuestionId)) {
			log.info("会话:{}, 追问轮次:{} 添加追问上下文到追问轮次:{}", conversationId, roundId, followUpQuestionId);
			this.chatMemory.addToRound(conversationId, followUpQuestionId, assistantMessages);
		} else {
			log.info("会话:{}, 对话轮次:{} 添加上下文到对话轮次:{}", conversationId, roundId, roundId);
			this.chatMemory.addToRound(conversationId, roundId, assistantMessages);
		}
		return chatClientResponse;
	}

	@NotNull
	@Override
	public Flux<ChatClientResponse> adviseStream(@NotNull ChatClientRequest chatClientRequest,
												 StreamAdvisorChain streamAdvisorChain) {
		// Get the scheduler from BaseAdvisor
		Scheduler scheduler = this.getScheduler();

		// Process the request with the before method
		return Mono.just(chatClientRequest)
				.publishOn(scheduler)
				.map(request -> this.before(request, streamAdvisorChain))
				.flatMapMany(streamAdvisorChain::nextStream)
				.transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux,
						response -> this.after(response, streamAdvisorChain)));
	}

	public void putToolMessage(Boolean followUpQuestion, String conversationId, String roundId,
							   String followUpQuestionId, String followUpQuestionReferenceId, List<Message> messages) {
		List<Message> curMessages = getMemoryMessages(followUpQuestion, conversationId, followUpQuestionId, followUpQuestionReferenceId);
		List<Message> filterMessages = messages.stream()
				.filter(message -> {
					Optional<Message> first = curMessages.stream()
							.filter(curMessage -> curMessage.hashCode() == message.hashCode())
							.findFirst();
					return first.isEmpty();
				}).toList();
		for (Message message : filterMessages) {
			if (MessageType.ASSISTANT.equals(message.getMessageType()) || MessageType.TOOL.equals(message.getMessageType())) {
				if (StrUtil.isNotBlank(followUpQuestionId)) {
					log.info("添加不存在的 ASSISTANT & TOOL 上下文信息到追问中, 会话Id:{}, 对话轮次Id:{} 追问Id:{} 完成", conversationId, roundId, followUpQuestionId);
					this.chatMemory.addToRound(conversationId, followUpQuestionId, message);
				} else {
					log.info("添加不存在的 ASSISTANT & TOOL 上下文信息到会话中, 会话Id:{}, 对话轮次Id:{} 完成", conversationId, roundId);
					this.chatMemory.addToRound(conversationId, roundId, message);
				}
			}
		}
	}

	public static UranMessageChatMemoryAdvisor.Builder builder(RoundBasedChatMemory chatMemory) {
		return new UranMessageChatMemoryAdvisor.Builder(chatMemory);
	}

	public static final class Builder {

		private String conversationId = RoundBasedChatMemory.DEFAULT_CONVERSATION_ID;

		private String roundId = RoundBasedChatMemory.DEFAULT_ROUND_ID;

		private String followUpQuestionId = RoundBasedChatMemory.DEFAULT_FOLLOW_UP_QUESTION_ID;

		private Boolean followUpQuestion = RoundBasedChatMemory.DEFAULT_FOLLOW_UP_QUESTION;

		private String followUpQuestionReferenceId = RoundBasedChatMemory.DEFAULT_FOLLOW_UP_QUESTION_REFERENCE_ID;

		private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

		private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;

		private final RoundBasedChatMemory chatMemory;

		private Builder(RoundBasedChatMemory chatMemory) {
			this.chatMemory = chatMemory;
		}

		/**
		 * Set the conversation id.
		 *
		 * @param conversationId the conversation id
		 * @return the builder
		 */
		public UranMessageChatMemoryAdvisor.Builder conversationId(String conversationId) {
			this.conversationId = conversationId;
			return this;
		}

		public UranMessageChatMemoryAdvisor.Builder followUpQuestionReferenceId(String followUpQuestionReferenceId) {
			this.followUpQuestionReferenceId = followUpQuestionReferenceId;
			return this;
		}

		public UranMessageChatMemoryAdvisor.Builder roundId(String roundId) {
			this.roundId = roundId;
			return this;
		}

		public UranMessageChatMemoryAdvisor.Builder followUpQuestionId(String followUpQuestionId) {
			this.followUpQuestionId = followUpQuestionId;
			return this;
		}

		public UranMessageChatMemoryAdvisor.Builder followUpQuestion(Boolean followUpQuestion) {
			this.followUpQuestion = followUpQuestion;
			return this;
		}

		/**
		 * Set the order.
		 *
		 * @param order the order
		 * @return the builder
		 */
		public UranMessageChatMemoryAdvisor.Builder order(int order) {
			this.order = order;
			return this;
		}

		public UranMessageChatMemoryAdvisor.Builder scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		/**
		 * Build the advisor.
		 *
		 * @return the advisor
		 */
		public UranMessageChatMemoryAdvisor build() {
			UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor = new UranMessageChatMemoryAdvisor();
			uranMessageChatMemoryAdvisor.chatMemory = this.chatMemory;
			uranMessageChatMemoryAdvisor.defaultConversationId = this.conversationId;
			uranMessageChatMemoryAdvisor.defaultRoundId = this.roundId;
			uranMessageChatMemoryAdvisor.defaultFollowUpQuestionId = this.followUpQuestionId;
			uranMessageChatMemoryAdvisor.defaultFollowUpQuestion = this.followUpQuestion;
			uranMessageChatMemoryAdvisor.defaultFollowUpQuestionReferenceId = this.followUpQuestionReferenceId;
			uranMessageChatMemoryAdvisor.order = this.order;
			uranMessageChatMemoryAdvisor.scheduler = this.scheduler;
			return uranMessageChatMemoryAdvisor;
		}
	}
}
