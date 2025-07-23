package com.ral.young.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ral.young.advisor.UranMessageChatMemoryAdvisor;
import com.ral.young.memeory.RoundBasedChatMemory;
import io.micrometer.observation.ObservationRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.observation.DefaultToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationDocumentation;
import org.springframework.ai.tool.resolution.DelegatingToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author renyh
 * @description 自定义 工具调用管理器
 * @date 2025/7/22 10:06
 * @since 1.0.0
 */
@SuppressWarnings("preview")
public class CustomToolCallingManager implements ToolCallingManager {

	private static final Logger logger = LoggerFactory.getLogger(CustomToolCallingManager.class);

	// @formatter:off

	private static final ObservationRegistry DEFAULT_OBSERVATION_REGISTRY
			= ObservationRegistry.NOOP;

	private static final ToolCallingObservationConvention DEFAULT_OBSERVATION_CONVENTION
			= new DefaultToolCallingObservationConvention();

	private static final ToolCallbackResolver DEFAULT_TOOL_CALLBACK_RESOLVER
			= new DelegatingToolCallbackResolver(List.of());

	private static final ToolExecutionExceptionProcessor DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR
			= DefaultToolExecutionExceptionProcessor.builder().build();

	// @formatter:on

	private final ObservationRegistry observationRegistry;

	private final ToolCallbackResolver toolCallbackResolver;

	private final ToolExecutionExceptionProcessor toolExecutionExceptionProcessor;

	private ToolCallingObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	private final UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor;

	public CustomToolCallingManager(ObservationRegistry observationRegistry, ToolCallbackResolver toolCallbackResolver,
									ToolExecutionExceptionProcessor toolExecutionExceptionProcessor, UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor) {
		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
		Assert.notNull(toolCallbackResolver, "toolCallbackResolver cannot be null");
		Assert.notNull(toolExecutionExceptionProcessor, "toolCallExceptionConverter cannot be null");

		this.observationRegistry = observationRegistry;
		this.toolCallbackResolver = toolCallbackResolver;
		this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
		this.uranMessageChatMemoryAdvisor = uranMessageChatMemoryAdvisor;
	}

	@NotNull
	@Override
	public List<ToolDefinition> resolveToolDefinitions(@NotNull ToolCallingChatOptions chatOptions) {
		Assert.notNull(chatOptions, "chatOptions cannot be null");

		List<ToolCallback> toolCallbacks = new ArrayList<>(chatOptions.getToolCallbacks());
		for (String toolName : chatOptions.getToolNames()) {
			// Skip the tool if it is already present in the request toolCallbacks.
			// That might happen if a tool is defined in the options
			// both as a ToolCallback and as a tool name.
			if (chatOptions.getToolCallbacks()
					.stream()
					.anyMatch(tool -> tool.getToolDefinition().name().equals(toolName))) {
				continue;
			}
			ToolCallback toolCallback = this.toolCallbackResolver.resolve(toolName);
			if (toolCallback == null) {
				throw new IllegalStateException(STR."No ToolCallback found for tool name: \{toolName}");
			}
			toolCallbacks.add(toolCallback);
		}

		return toolCallbacks.stream().map(ToolCallback::getToolDefinition).toList();
	}

	@NotNull
	@Override
	public ToolExecutionResult executeToolCalls(@NotNull Prompt prompt, @NotNull ChatResponse chatResponse) {
		Assert.notNull(prompt, "prompt cannot be null");
		Assert.notNull(chatResponse, "chatResponse cannot be null");

		Optional<Generation> toolCallGeneration = chatResponse.getResults()
				.stream()
				.filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
				.findFirst();

		if (toolCallGeneration.isEmpty()) {
			logger.info("No tool call requested by the chat model");
			throw new IllegalStateException("No tool call requested by the chat model");
		}
		AssistantMessage assistantMessage = toolCallGeneration.get().getOutput();

		ToolContext toolContext = buildToolContext(prompt, assistantMessage);

		CustomToolCallingManager.InternalToolExecutionResult internalToolExecutionResult = executeToolCall(prompt, assistantMessage,
				toolContext);

		List<Message> conversationHistory = buildConversationHistoryAfterToolExecution(prompt.getInstructions(),
				assistantMessage, internalToolExecutionResult.toolResponseMessage());

		var options = prompt.getOptions();
		if (options instanceof ToolCallingChatOptions toolCallingChatOptions) {
			// 将 tool 调用过程中的消息存储上下文
			pushToolMessageToContext(toolCallingChatOptions, conversationHistory);
		}

		return ToolExecutionResult.builder()
				.conversationHistory(conversationHistory)
				.returnDirect(internalToolExecutionResult.returnDirect())
				.build();
	}

	private void pushToolMessageToContext(ToolCallingChatOptions toolCallingChatOptions, List<Message> conversationHistory) {
		Map<String, Object> optionsToolContext = toolCallingChatOptions.getToolContext();
		String conversationId = (String) optionsToolContext.get(RoundBasedChatMemory.CONVERSATION_ID);
		String roundId = (String) optionsToolContext.get(RoundBasedChatMemory.ROUND_ID);
		String flowQuestionId = (String) optionsToolContext.getOrDefault(RoundBasedChatMemory.FOLLOW_UP_QUESTION_ID, StrUtil.EMPTY);
		String followUpQuestionReferenceId = (String) optionsToolContext.getOrDefault(RoundBasedChatMemory.FOLLOW_UP_QUESTION_REFERENCE_ID, StrUtil.EMPTY);
		Boolean followUpQuestion = (Boolean) optionsToolContext.getOrDefault(RoundBasedChatMemory.FOLLOW_UP_QUESTION, false);
		if (CollUtil.isNotEmpty(conversationHistory)) {
			uranMessageChatMemoryAdvisor.putToolMessage(followUpQuestion, conversationId, roundId, flowQuestionId, followUpQuestionReferenceId, conversationHistory);
		}
	}

	private static ToolContext buildToolContext(Prompt prompt, AssistantMessage assistantMessage) {
		Map<String, Object> toolContextMap = Map.of();

		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions
				&& !CollectionUtils.isEmpty(toolCallingChatOptions.getToolContext())) {
			toolContextMap = new HashMap<>(toolCallingChatOptions.getToolContext());

			List<Message> messageHistory = new ArrayList<>(prompt.copy().getInstructions());
			messageHistory.add(new AssistantMessage(Objects.requireNonNull(assistantMessage.getText()), assistantMessage.getMetadata(),
					assistantMessage.getToolCalls()));

			toolContextMap.put(ToolContext.TOOL_CALL_HISTORY,
					buildConversationHistoryBeforeToolExecution(prompt, assistantMessage));
		}

		return new ToolContext(toolContextMap);
	}

	private static List<Message> buildConversationHistoryBeforeToolExecution(Prompt prompt,
																			 AssistantMessage assistantMessage) {
		List<Message> messageHistory = new ArrayList<>(prompt.copy().getInstructions());
		messageHistory.add(new AssistantMessage(Objects.requireNonNull(assistantMessage.getText()), assistantMessage.getMetadata(),
				assistantMessage.getToolCalls()));
		return messageHistory;
	}

	/**
	 * Execute the tool call and return the response message.
	 */
	private CustomToolCallingManager.InternalToolExecutionResult executeToolCall(Prompt prompt, AssistantMessage assistantMessage,
																				 ToolContext toolContext) {
		List<ToolCallback> toolCallbacks = List.of();
		if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
			toolCallbacks = toolCallingChatOptions.getToolCallbacks();
		}

		List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

		Boolean returnDirect = null;
		for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
			logger.info("待执行的工具名称: {}, 参数信息: {}", toolCall.name(), toolCall.arguments());

			String toolName = toolCall.name();
			String toolInputArguments = toolCall.arguments();

			ToolCallback toolCallback = toolCallbacks.stream()
					.filter(tool -> toolName.equals(tool.getToolDefinition().name()))
					.findFirst()
					.orElseGet(() -> this.toolCallbackResolver.resolve(toolName));

			if (toolCallback == null) {
				throw new IllegalStateException(STR."No ToolCallback found for tool name: \{toolName}");
			}

			if (returnDirect == null) {
				returnDirect = toolCallback.getToolMetadata().returnDirect();
			} else {
				returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
			}

			ToolCallingObservationContext observationContext = ToolCallingObservationContext.builder()
					.toolDefinition(toolCallback.getToolDefinition())
					.toolMetadata(toolCallback.getToolMetadata())
					.toolCallArguments(toolInputArguments)
					.build();

			String toolCallResult = ToolCallingObservationDocumentation.TOOL_CALL
					.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
							this.observationRegistry)
					.observe(() -> {
						String toolResult;
						try {
							toolResult = toolCallback.call(toolInputArguments, toolContext);
						} catch (ToolExecutionException ex) {
							toolResult = this.toolExecutionExceptionProcessor.process(ex);
						}
						observationContext.setToolCallResult(toolResult);
						return toolResult;
					});

			toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName,
					toolCallResult != null ? toolCallResult : ""));
		}

		return new CustomToolCallingManager.InternalToolExecutionResult(new ToolResponseMessage(toolResponses, Map.of()), returnDirect);
	}

	private List<Message> buildConversationHistoryAfterToolExecution(List<Message> previousMessages,
																	 AssistantMessage assistantMessage, ToolResponseMessage toolResponseMessage) {
		List<Message> messages = new ArrayList<>(previousMessages);
		messages.add(assistantMessage);
		messages.add(toolResponseMessage);
		return messages;
	}

	public void setObservationConvention(ToolCallingObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

	public static CustomToolCallingManager.Builder builder() {
		return new CustomToolCallingManager.Builder();
	}

	private record InternalToolExecutionResult(ToolResponseMessage toolResponseMessage, boolean returnDirect) {
	}

	public final static class Builder {

		private ObservationRegistry observationRegistry = DEFAULT_OBSERVATION_REGISTRY;

		private ToolCallbackResolver toolCallbackResolver = DEFAULT_TOOL_CALLBACK_RESOLVER;

		private ToolExecutionExceptionProcessor toolExecutionExceptionProcessor = DEFAULT_TOOL_EXECUTION_EXCEPTION_PROCESSOR;

		private UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor;

		private Builder() {
		}

		public CustomToolCallingManager.Builder observationRegistry(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
			return this;
		}

		public CustomToolCallingManager.Builder toolCallbackResolver(ToolCallbackResolver toolCallbackResolver) {
			this.toolCallbackResolver = toolCallbackResolver;
			return this;
		}

		public CustomToolCallingManager.Builder toolExecutionExceptionProcessor(
				ToolExecutionExceptionProcessor toolExecutionExceptionProcessor) {
			this.toolExecutionExceptionProcessor = toolExecutionExceptionProcessor;
			return this;
		}

		public CustomToolCallingManager.Builder messageChatMemoryAdvisor(
				UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor) {
			this.uranMessageChatMemoryAdvisor = uranMessageChatMemoryAdvisor;
			return this;
		}

		public CustomToolCallingManager build() {
			return new CustomToolCallingManager(this.observationRegistry, this.toolCallbackResolver,
					this.toolExecutionExceptionProcessor, uranMessageChatMemoryAdvisor);
		}

	}

}
