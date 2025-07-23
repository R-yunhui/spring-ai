package com.ral.young.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ral.young.advisor.UranMessageChatMemoryAdvisor;
import com.ral.young.common.CommonConstants;
import com.ral.young.dto.request.VideoSearchRequest;
import com.ral.young.memeory.RoundBasedChatMemory;
import com.ral.young.model.UranBedrockProxyChatModel;
import com.ral.young.vo.ChatRequestVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author renyh
 * @description 视频检索demo
 * @date 2025/7/19 15:08
 * @since 1.0.0
 */
@Service
@Slf4j
@SuppressWarnings("preview")
public class VideoSearchService {

	@Value(value = "${prompt}")
	private String prompt;

	private final ChatClient chatClient;

	private final ChatClient awsChatClient;

	private final List<ToolCallbackProvider> tools;

	private final UranBedrockProxyChatModel uranBedrockProxyChatModel;

	public final UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor;


	public VideoSearchService(DashScopeChatModel dashScopeChatModel, List<ToolCallbackProvider> tools,
							  UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor, UranBedrockProxyChatModel uranBedrockProxyChatModel) {
		this.uranBedrockProxyChatModel = uranBedrockProxyChatModel;
		this.awsChatClient = ChatClient.builder(uranBedrockProxyChatModel)
				.defaultAdvisors(uranMessageChatMemoryAdvisor)
				.build();
		this.tools = tools;
		this.uranMessageChatMemoryAdvisor = uranMessageChatMemoryAdvisor;
		this.chatClient = ChatClient.builder(dashScopeChatModel)
				.defaultAdvisors(uranMessageChatMemoryAdvisor)
				.build();
	}

	public void testAws() {
		var options = ToolCallingChatOptions.builder()
				.model("us.anthropic.claude-3-7-sonnet-20250219-v1:0")
				.temperature(0.6)
				.maxTokens(10000)
				// .toolCallbacks(getToolCallbacks())
				.build();

		String response = ChatClient.create(this.uranBedrockProxyChatModel)
				.prompt("你是谁？")
				.options(options)
				.call()
				.content();
		System.out.println(response);
	}

	public ChatResponse videoSearch(ChatRequestVO chatRequestVO) {
		return callLlmNotStream(chatRequestVO, prompt);
	}

	public Flux<CustomChatResponse> videoSearchStream(ChatRequestVO chatRequestVO) {
		return callLlm(chatRequestVO, prompt);
	}

	@NotNull
	private Flux<CustomChatResponse> callLlm(ChatRequestVO chatRequestVO, String systemPrompt) {
		SystemMessage systemMessage = new SystemMessage(systemPrompt);
		String userPrompt = STR."""
				视频ID列表：\{chatRequestVO.getVideoIds()}
				异步任务ID：\{chatRequestVO.getTaskId()}
				用户问题：%s \{chatRequestVO.getPrompt()}
				""";
		UserMessage userMessage = new UserMessage(userPrompt);
		userMessage.getMetadata().put(ChatMemory.CONVERSATION_ID, chatRequestVO.getConversationId());
		Map<String, Object> paramMap = buildParamMap(chatRequestVO);
		var options = ToolCallingChatOptions.builder()
				.model("us.anthropic.claude-3-7-sonnet-20250219-v1:0")
				.temperature(1.0)
				.maxTokens(10000)
				.toolCallbacks(getToolCallbacks())
				.toolContext(paramMap)
				.build();
		return awsChatClient
				.prompt(new Prompt(List.of(systemMessage, userMessage)))
				.toolContext(paramMap)
				.options(options)
				.toolCallbacks(getToolCallbacks())
				.advisors(advisor ->
						advisor.params(paramMap)
				)
				.stream()
				.chatClientResponse()
				.map(response -> convertToCustomResponse(response, chatRequestVO))
				.filter(customChatResponse -> !StrUtil.isAllBlank(customChatResponse.getContent(), customChatResponse.getReasonContent()))
				.concatWith(Flux.just(CustomChatResponse.builder()
						.content(StrUtil.EMPTY)
						.reasonContent(StrUtil.EMPTY)
						.timestamp(System.currentTimeMillis())
						.complete(true)
						.build()))
				.doOnComplete(() -> log.info("video search stream complete"));
	}

	private Map<String, Object> buildParamMap(ChatRequestVO chatRequestVO) {
		Map<String, Object> paramsMap = new HashMap<>(16);
		String roundId = IdUtil.fastSimpleUUID();
		String followUpQuestionId = chatRequestVO.getFollowUpQuestionId();
		String conversationId = chatRequestVO.getConversationId();
		String followUpQuestionReferenceId = chatRequestVO.getFollowUpQuestionReferenceId();
		Boolean followUpQuestion = chatRequestVO.getFollowUpQuestion();
		if (Boolean.TRUE.equals(followUpQuestion) && StrUtil.isBlank(followUpQuestionId)) {
			followUpQuestionId = uranMessageChatMemoryAdvisor.chatMemory.createFollowupRound(conversationId, roundId);
			// 后续的轮次 id 使用依赖的对话 id 轮次获取对应的完整上下文进行存储
			List<Message> refRoundMessages = uranMessageChatMemoryAdvisor.chatMemory.getRound(conversationId, followUpQuestionReferenceId);
			// 添加到新的里面去
			uranMessageChatMemoryAdvisor.chatMemory.addToRound(conversationId, followUpQuestionId, refRoundMessages);
			log.info("会话:{} 对话轮次:{} 追问依赖的对话轮次:{}, 追问对话轮次:{}, 将依赖的对话轮次上下文添加到追问对话轮次中, 添加的数量: {}",
					conversationId, roundId, followUpQuestionReferenceId, followUpQuestionId, refRoundMessages.size());
		}
		paramsMap.put(RoundBasedChatMemory.CONVERSATION_ID, conversationId);
		paramsMap.put(RoundBasedChatMemory.ROUND_ID, roundId);
		paramsMap.put(RoundBasedChatMemory.FOLLOW_UP_QUESTION_ID, followUpQuestionId);
		paramsMap.put(RoundBasedChatMemory.FOLLOW_UP_QUESTION, Optional.ofNullable(followUpQuestion).orElse(false));
		paramsMap.put(RoundBasedChatMemory.FOLLOW_UP_QUESTION_REFERENCE_ID, Optional.ofNullable(followUpQuestionReferenceId).orElse(StrUtil.EMPTY));
		paramsMap.put(CommonConstants.PROMPT, chatRequestVO.getPrompt());
		paramsMap.put(CommonConstants.THINKING, true);
		return paramsMap;
	}

	@NotNull
	private ChatResponse callLlmNotStream(ChatRequestVO chatRequestVO, String systemPrompt) {
		List<ToolCallback> toolCallbacks = getToolCallbacks();
		// 获取用户查询
		String userQuery = chatRequestVO.getPrompt();
		// 构建用户提示
		SystemMessage systemMessage = new SystemMessage(systemPrompt);
		String userPrompt = STR."""
				视频ID列表：\{chatRequestVO.getVideoIds()}
				异步任务ID：\{chatRequestVO.getTaskId()}
				用户问题：%s \{chatRequestVO.getPrompt()}
				""";
		UserMessage userMessage = new UserMessage(userPrompt);
		userMessage.getMetadata().put(ChatMemory.CONVERSATION_ID, chatRequestVO.getConversationId());
		var options = ToolCallingChatOptions.builder()
				.model("us.anthropic.claude-3-7-sonnet-20250219-v1:0")
				.temperature(1.0)
				.maxTokens(10000)
				.toolCallbacks(toolCallbacks)
				.build();
		return Objects.requireNonNull(awsChatClient
				.prompt(new Prompt(List.of(systemMessage, userMessage)))
				.toolContext(Map.of(ChatMemory.CONVERSATION_ID, chatRequestVO.getConversationId(), "originalPrompt", userQuery))
				.options(options)
				.toolCallbacks(toolCallbacks)
				.advisors(advisor ->
						advisor.param(ChatMemory.CONVERSATION_ID, chatRequestVO.getConversationId())
				).call()
				.chatResponse());
	}

	private @NotNull List<ToolCallback> getToolCallbacks() {
		return tools.stream()
				.map(ToolCallbackProvider::getToolCallbacks)
				.flatMap(Arrays::stream)
				.toList();
	}

	public @Nullable String getMarkdownReport(SystemMessage systemMessage, UserMessage userMessage) {
		ChatResponse chatResponse = chatClient
				.prompt(new Prompt(List.of(systemMessage, userMessage)))
				.options(DashScopeChatOptions.builder()
						.withModel(DashScopeApi.ChatModel.QWEN_MAX.getModel())
						.withTemperature(0.7).build())
				.call()
				.chatResponse();

		assert chatResponse != null;
		return chatResponse.getResult()
				.getOutput()
				.getText();
	}

	public void notifyTaskCompletion(Long taskId, Long chatId) {
		// 发送通知给用户
		// 继续后续的处理
		String content = chatClient
				.prompt()
				.toolContext(Map.of(ChatMemory.CONVERSATION_ID, chatId))
				.options(DashScopeChatOptions.builder()
						.withModel(DashScopeApi.ChatModel.QWEN_PLUS.getModel())
						.withMaxToken(16384)
						.withTemperature(0.7).build())
				.advisors(advisor ->
						advisor.param(ChatMemory.CONVERSATION_ID, chatId)
				)
				.toolCallbacks(getToolCallbacks())
				.call()
				.content();
		log.info("任务完成，结果为：{}", content);
	}


	/**
	 * 自定义聊天响应类
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CustomChatResponse {
		/**
		 * 内容
		 */
		private String content;
		/**
		 * 思考内容
		 */
		private String reasonContent;
		/**
		 * 时间戳
		 */
		private Long timestamp;
		/**
		 * 是否完成
		 */
		private Boolean complete;
		/**
		 * 会话id
		 */
		private String conversationId;
		/**
		 * 轮次id
		 */
		private String roundId;

		/**
		 * 追问轮次id
		 */
		private String followUpQuestionId;
	}

	/**
	 * 将ChatResponse转换为CustomChatResponse
	 */
	private CustomChatResponse convertToCustomResponse(ChatClientResponse chatClientResponse, ChatRequestVO chatRequestVO) {
		String content = StrUtil.EMPTY;
		ChatResponse response = chatClientResponse.chatResponse();
		if (ObjectUtil.isNotNull(response) && ObjectUtil.isNotNull(response.getResult()) && ObjectUtil.isNotNull(response.getResult().getOutput())) {
			content = response.getResult().getOutput().getText();
		}

		String reasonContent = StrUtil.EMPTY;
		if (ObjectUtil.isNotNull(response) && ObjectUtil.isNotNull(response.getResult()) && ObjectUtil.isNotNull(response.getResult().getMetadata())) {
			reasonContent = response.getResult().getMetadata().getOrDefault(CommonConstants.REASONING_CONTENT, StrUtil.EMPTY);
		}

		String roundId = chatClientResponse.context().get(RoundBasedChatMemory.ROUND_ID).toString();
		String followUpQuestionId = chatClientResponse.context().get(RoundBasedChatMemory.FOLLOW_UP_QUESTION_ID).toString();
		return CustomChatResponse.builder()
				.content(content)
				.reasonContent(reasonContent)
				.timestamp(System.currentTimeMillis())
				.complete(false)
				.conversationId(chatRequestVO.getConversationId())
				.roundId(roundId)
				.followUpQuestionId(followUpQuestionId)
				.build();
	}
}