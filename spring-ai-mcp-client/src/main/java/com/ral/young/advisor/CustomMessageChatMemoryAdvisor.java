package com.ral.young.advisor;

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
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author renyh
 * @description 自定义 CustomMessageChatMemoryAdvisor
 * @date 2025/7/22 9:58
 * @since 1.0.0
 */
@Slf4j
@Service
public class CustomMessageChatMemoryAdvisor implements BaseChatMemoryAdvisor {

	private  ChatMemory chatMemory;

	private  String defaultConversationId;

	private  int order;

	private  Scheduler scheduler;

	@Override
	public int getOrder() {
		return this.order;
	}

	@NotNull
	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@NotNull
	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, @NotNull AdvisorChain advisorChain) {
		String conversationId = getConversationId(chatClientRequest.context(), this.defaultConversationId);

		// 1. Retrieve the chat memory for the current conversation.
		List<Message> memoryMessages = this.chatMemory.get(conversationId);

		// 2. Advise the request messages list.
		List<Message> processedMessages = new ArrayList<>(memoryMessages);
		processedMessages.addAll(chatClientRequest.prompt().getInstructions());

		// 3. Create a new request with the advised messages.
		ChatClientRequest processedChatClientRequest = chatClientRequest.mutate()
				.prompt(chatClientRequest.prompt().mutate().messages(processedMessages).build())
				.build();

		// 4. Add the new user message to the conversation memory.
		UserMessage userMessage = processedChatClientRequest.prompt().getUserMessage();
		this.chatMemory.add(conversationId, userMessage);

		return processedChatClientRequest;
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
		this.chatMemory.add(this.getConversationId(chatClientResponse.context(), this.defaultConversationId),
				assistantMessages);
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

	public void putToolMessage(String chatId, List<Message> messages) {
		List<Message> curMessage = this.chatMemory.get(chatId);
		log.info("添加前的消息数量:{}", curMessage.size());
		for (Message message : messages) {
			if (curMessage.hashCode() == message.hashCode()) {
				continue;
			}
			this.chatMemory.add(chatId, message);
		}
		log.info("添加后的消息数量:{}", this.chatMemory.get(chatId).size());
	}

	public static CustomMessageChatMemoryAdvisor.Builder builder(ChatMemory chatMemory) {
		return new CustomMessageChatMemoryAdvisor.Builder(chatMemory);
	}

	public static final class Builder {

		private String conversationId = ChatMemory.DEFAULT_CONVERSATION_ID;

		private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;

		private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;

		private final ChatMemory chatMemory;

		private Builder(ChatMemory chatMemory) {
			this.chatMemory = chatMemory;
		}

		/**
		 * Set the conversation id.
		 *
		 * @param conversationId the conversation id
		 * @return the builder
		 */
		public CustomMessageChatMemoryAdvisor.Builder conversationId(String conversationId) {
			this.conversationId = conversationId;
			return this;
		}

		/**
		 * Set the order.
		 *
		 * @param order the order
		 * @return the builder
		 */
		public CustomMessageChatMemoryAdvisor.Builder order(int order) {
			this.order = order;
			return this;
		}

		public CustomMessageChatMemoryAdvisor.Builder scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		/**
		 * Build the advisor.
		 *
		 * @return the advisor
		 */
		public CustomMessageChatMemoryAdvisor build() {
			CustomMessageChatMemoryAdvisor customMessageChatMemoryAdvisor = new CustomMessageChatMemoryAdvisor();
			customMessageChatMemoryAdvisor.chatMemory = this.chatMemory;
			customMessageChatMemoryAdvisor.defaultConversationId = this.conversationId;
			customMessageChatMemoryAdvisor.order = this.order;
			customMessageChatMemoryAdvisor.scheduler = this.scheduler;
			return customMessageChatMemoryAdvisor;
		}
	}
}
