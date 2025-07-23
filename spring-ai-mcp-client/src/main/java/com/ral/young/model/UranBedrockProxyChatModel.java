package com.ral.young.model;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ral.young.common.CommonConstants;
import com.ral.young.memeory.RoundBasedChatMemory;
import com.ral.young.utils.UranConverseApiUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.api.BedrockMediaFormat;
import org.springframework.ai.bedrock.converse.api.ConverseApiUtils;
import org.springframework.ai.bedrock.converse.api.URLValidator;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.ModelOptions;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseMetrics;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.DocumentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.DocumentSource;
import software.amazon.awssdk.services.bedrockruntime.model.ImageBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ImageSource;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.ReasoningContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ReasoningTextBlock;
import software.amazon.awssdk.services.bedrockruntime.model.S3Location;
import software.amazon.awssdk.services.bedrockruntime.model.StopReason;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;
import software.amazon.awssdk.services.bedrockruntime.model.ToolUseBlock;
import software.amazon.awssdk.services.bedrockruntime.model.VideoBlock;
import software.amazon.awssdk.services.bedrockruntime.model.VideoFormat;
import software.amazon.awssdk.services.bedrockruntime.model.VideoSource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author renyh
 * @description 自定义 BedrockProxyChatModel
 * @date 2025/7/24 20:05
 * @since 1.0.0
 */
public class UranBedrockProxyChatModel implements ChatModel {

	private static final Logger logger = LoggerFactory.getLogger(UranBedrockProxyChatModel.class);

	private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

	private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();

	private final BedrockRuntimeClient bedrockRuntimeClient;

	private final BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient;

	private final ToolCallingChatOptions defaultOptions;

	/**
	 * 存放思考信息的 map
	 * 如果 tool + thinking 需要保证 thinking block 在 tool info block 前面
	 */
	public static ConcurrentHashMap<String, Map<String, String>> thinkingInfoMap = new ConcurrentHashMap<>();

	/**
	 * Observation registry used for instrumentation.
	 */
	private final ObservationRegistry observationRegistry;

	private final ToolCallingManager toolCallingManager;

	/**
	 * The tool execution eligibility predicate used to determine if a tool can be
	 * executed.
	 */
	private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;

	/**
	 * Conventions to use for generating observations.
	 */
	private ChatModelObservationConvention observationConvention;

	public UranBedrockProxyChatModel(BedrockRuntimeClient bedrockRuntimeClient,
									 BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient, ToolCallingChatOptions defaultOptions,
									 ObservationRegistry observationRegistry, ToolCallingManager toolCallingManager) {
		this(bedrockRuntimeClient, bedrockRuntimeAsyncClient, defaultOptions, observationRegistry, toolCallingManager,
				new DefaultToolExecutionEligibilityPredicate());
	}

	public UranBedrockProxyChatModel(BedrockRuntimeClient bedrockRuntimeClient,
									 BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient, ToolCallingChatOptions defaultOptions,
									 ObservationRegistry observationRegistry, ToolCallingManager toolCallingManager,
									 ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {

		Assert.notNull(bedrockRuntimeClient, "bedrockRuntimeClient must not be null");
		Assert.notNull(bedrockRuntimeAsyncClient, "bedrockRuntimeAsyncClient must not be null");
		Assert.notNull(toolCallingManager, "toolCallingManager must not be null");
		Assert.notNull(toolExecutionEligibilityPredicate, "toolExecutionEligibilityPredicate must not be null");

		this.bedrockRuntimeClient = bedrockRuntimeClient;
		this.bedrockRuntimeAsyncClient = bedrockRuntimeAsyncClient;
		this.defaultOptions = defaultOptions;
		this.observationRegistry = observationRegistry;
		this.toolCallingManager = toolCallingManager;
		this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
	}

	private static ToolCallingChatOptions from(ChatOptions options) {
		return ToolCallingChatOptions.builder()
				.model(options.getModel())
				.maxTokens(options.getMaxTokens())
				.stopSequences(options.getStopSequences())
				.temperature(options.getTemperature())
				.topP(options.getTopP())
				.build();
	}

	/**
	 * Invoke the model and return the response.
	 * <p>
	 * https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters.html
	 * https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html
	 * https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/bedrockruntime/BedrockRuntimeClient.html#converse
	 *
	 * @return The model invocation response.
	 */
	@Override
	public ChatResponse call(Prompt prompt) {
		Prompt requestPrompt = buildRequestPrompt(prompt);
		return this.internalCall(requestPrompt, null);
	}

	private ChatResponse internalCall(Prompt prompt, ChatResponse perviousChatResponse) {

		ConverseRequest converseRequest = this.createRequest(prompt);

		ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
				.prompt(prompt)
				.provider(AiProvider.BEDROCK_CONVERSE.value())
				.build();

		ChatResponse chatResponse = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
				.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
						this.observationRegistry)
				.observe(() -> {
					ConverseResponse converseResponse = this.bedrockRuntimeClient.converse(converseRequest);

					logger.debug("ConverseResponse: {}", converseResponse);

					var response = this.toChatResponse(converseResponse, perviousChatResponse);

					observationContext.setResponse(response);

					return response;
				});

		if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), chatResponse)
				&& chatResponse.hasFinishReasons(Set.of(StopReason.TOOL_USE.toString()))) {
			var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, chatResponse);
			if (toolExecutionResult.returnDirect()) {
				// Return tool execution result directly to the client.
				return ChatResponse.builder()
						.from(chatResponse)
						.generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
						.build();
			} else {
				// Send the tool execution result back to the model.
				return this.internalCall(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()),
						chatResponse);
			}
		}
		return chatResponse;
	}

	@Override
	public ChatOptions getDefaultOptions() {
		return this.defaultOptions;
	}

	Prompt buildRequestPrompt(Prompt prompt) {
		ToolCallingChatOptions runtimeOptions = null;
		if (prompt.getOptions() != null) {
			if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
				runtimeOptions = toolCallingChatOptions.copy();
			} else {
				runtimeOptions = from(prompt.getOptions());
			}
		}

		// Merge runtime options with the default options
		ToolCallingChatOptions updatedRuntimeOptions = null;
		if (runtimeOptions == null) {
			updatedRuntimeOptions = this.defaultOptions.copy();
		} else {
			if (runtimeOptions.getFrequencyPenalty() != null) {
				logger.warn("The frequencyPenalty option is not supported by UranBedrockProxyChatModel. Ignoring.");
			}
			if (runtimeOptions.getPresencePenalty() != null) {
				logger.warn("The presencePenalty option is not supported by UranBedrockProxyChatModel. Ignoring.");
			}
			if (runtimeOptions.getTopK() != null) {
				logger.warn("The topK option is not supported by UranBedrockProxyChatModel. Ignoring.");
			}
			updatedRuntimeOptions = ToolCallingChatOptions.builder()
					.model(runtimeOptions.getModel() != null ? runtimeOptions.getModel() : this.defaultOptions.getModel())
					.maxTokens(runtimeOptions.getMaxTokens() != null ? runtimeOptions.getMaxTokens()
							: this.defaultOptions.getMaxTokens())
					.stopSequences(runtimeOptions.getStopSequences() != null ? runtimeOptions.getStopSequences()
							: this.defaultOptions.getStopSequences())
					.temperature(runtimeOptions.getTemperature() != null ? runtimeOptions.getTemperature()
							: this.defaultOptions.getTemperature())
					.topP(runtimeOptions.getTopP() != null ? runtimeOptions.getTopP() : this.defaultOptions.getTopP())

					.toolCallbacks(runtimeOptions.getToolCallbacks() != null ? runtimeOptions.getToolCallbacks()
							: this.defaultOptions.getToolCallbacks())
					.toolNames(runtimeOptions.getToolNames() != null ? runtimeOptions.getToolNames()
							: this.defaultOptions.getToolNames())
					.toolContext(runtimeOptions.getToolContext() != null ? runtimeOptions.getToolContext()
							: this.defaultOptions.getToolContext())
					.internalToolExecutionEnabled(runtimeOptions.getInternalToolExecutionEnabled() != null
							? runtimeOptions.getInternalToolExecutionEnabled()
							: this.defaultOptions.getInternalToolExecutionEnabled())
					.build();
		}

		ToolCallingChatOptions.validateToolCallbacks(updatedRuntimeOptions.getToolCallbacks());

		return new Prompt(prompt.getInstructions(), updatedRuntimeOptions);
	}

	ConverseRequest createRequest(Prompt prompt) {
		String chatMemoryConversationId = getConversationId(prompt);
		List<Message> instructionMessages = prompt.getInstructions()
				.stream()
				.filter(message -> message.getMessageType() != MessageType.SYSTEM)
				.map(message -> {
					if (message.getMessageType() == MessageType.USER) {
						List<ContentBlock> contents = new ArrayList<>();
						if (message instanceof UserMessage) {
							var userMessage = (UserMessage) message;
							contents.add(ContentBlock.fromText(userMessage.getText()));

							if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
								List<ContentBlock> mediaContent = userMessage.getMedia()
										.stream()
										.map(this::mapMediaToContentBlock)
										.toList();
								contents.addAll(mediaContent);
							}
						}
						return Message.builder().content(contents).role(ConversationRole.USER).build();
					} else if (message.getMessageType() == MessageType.ASSISTANT) {
						AssistantMessage assistantMessage = (AssistantMessage) message;
						List<ContentBlock> contentBlocks = new ArrayList<>();
						if (StringUtils.hasText(message.getText())) {
							contentBlocks.add(ContentBlock.fromText(message.getText()));
						}
						if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
							for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
								var argumentsDocument = ConverseApiUtils
										.convertObjectToDocument(ModelOptionsUtils.jsonToMap(toolCall.arguments()));
								Map<String, String> metaMap = thinkingInfoMap.get(chatMemoryConversationId);
								ContentBlock reasoningContent = ContentBlock.fromReasoningContent(ReasoningContentBlock.builder()
										.reasoningText(ReasoningTextBlock.builder()
												.text(metaMap.getOrDefault(CommonConstants.REASONING_CONTENT, StrUtil.EMPTY))
												.signature(metaMap.getOrDefault(CommonConstants.SIGNATURE, StrUtil.EMPTY))
												.build())
										.build());
								contentBlocks.add(reasoningContent);
								ContentBlock toolContent = ContentBlock.fromToolUse(ToolUseBlock.builder()
										.toolUseId(toolCall.id())
										.name(toolCall.name())
										.input(argumentsDocument)
										.build());
								contentBlocks.add(toolContent);
							}
						}
						return Message.builder().content(contentBlocks).role(ConversationRole.ASSISTANT).build();
					} else if (message.getMessageType() == MessageType.TOOL) {
						List<ContentBlock> contentBlocks = ((ToolResponseMessage) message).getResponses()
								.stream()
								.map(toolResponse -> {
									ToolResultBlock toolResultBlock = ToolResultBlock.builder()
											.toolUseId(toolResponse.id())
											.content(ToolResultContentBlock.builder().text(toolResponse.responseData()).build())
											.build();
									return ContentBlock.fromToolResult(toolResultBlock);
								})
								.toList();
						return Message.builder().content(contentBlocks).role(ConversationRole.USER).build();
					} else {
						throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
					}
				})
				.toList();

		List<SystemContentBlock> systemMessages = prompt.getInstructions()
				.stream()
				.filter(m -> m.getMessageType() == MessageType.SYSTEM)
				.map(sysMessage -> SystemContentBlock.builder().text(sysMessage.getText()).build())
				.toList();

		ToolCallingChatOptions updatedRuntimeOptions = prompt.getOptions().copy();

		ToolConfiguration toolConfiguration = null;

		// Add the tool definitions to the request's tools parameter.
		List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(updatedRuntimeOptions);

		if (!CollectionUtils.isEmpty(toolDefinitions)) {
			List<Tool> bedrockTools = toolDefinitions.stream().map(toolDefinition -> {
				var description = toolDefinition.description();
				var name = toolDefinition.name();
				String inputSchema = toolDefinition.inputSchema();
				return Tool.builder()
						.toolSpec(ToolSpecification.builder()
								.name(name)
								.description(description)
								.inputSchema(ToolInputSchema.fromJson(
										ConverseApiUtils.convertObjectToDocument(ModelOptionsUtils.jsonToMap(inputSchema))))
								.build())
						.build();
			}).toList();

			toolConfiguration = ToolConfiguration.builder().tools(bedrockTools).build();
		}

		InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder()
				.maxTokens(updatedRuntimeOptions.getMaxTokens())
				.stopSequences(updatedRuntimeOptions.getStopSequences())
				.temperature(updatedRuntimeOptions.getTemperature() != null
						? updatedRuntimeOptions.getTemperature().floatValue() : null)
				.topP(updatedRuntimeOptions.getTopP() != null ? updatedRuntimeOptions.getTopP().floatValue() : null)
				.build();

		Document additionalModelRequestFields = getChatOptionsAdditionalModelRequestFields(this.defaultOptions, prompt.getOptions());
		return ConverseRequest.builder()
				.modelId(updatedRuntimeOptions.getModel())
				.inferenceConfig(inferenceConfiguration)
				.messages(instructionMessages)
				.system(systemMessages)
				.additionalModelRequestFields(additionalModelRequestFields)
				.toolConfig(toolConfiguration)
				.build();
	}

	public static Document getChatOptionsAdditionalModelRequestFields(ChatOptions defaultOptions,
																	  ModelOptions promptOptions) {
		if (defaultOptions == null && promptOptions == null) {
			return null;
		}

		Map<String, Object> attributes = new HashMap<>();

		if (defaultOptions != null) {
			attributes.putAll(ModelOptionsUtils.objectToMap(defaultOptions));
		}

		if (promptOptions != null) {
			if (promptOptions instanceof ChatOptions runtimeOptions) {
				attributes.putAll(ModelOptionsUtils.objectToMap(runtimeOptions));
			}
			else {
				throw new IllegalArgumentException(
						STR."Prompt options are not of type ChatOptions:\{promptOptions.getClass().getSimpleName()}");
			}
		}

		attributes.remove("model");
		attributes.remove("proxyToolCalls");
		attributes.remove("functions");
		attributes.remove("toolContext");
		attributes.remove("toolCallbacks");

		attributes.remove("toolCallbacks");
		attributes.remove("toolNames");
		attributes.remove("internalToolExecutionEnabled");

		attributes.remove("temperature");
		attributes.remove("topK");
		attributes.remove("stopSequences");
		attributes.remove("maxTokens");
		attributes.remove("topP");

		if (promptOptions instanceof ToolCallingChatOptions toolCallingChatOptions) {
			boolean thinking = (Boolean) toolCallingChatOptions.getToolContext().getOrDefault("thinking", false);
			if (thinking) {
				attributes.put("thinking", Map.of("type", "enabled", "budget_tokens", 2000));
				logger.info("当前请求打开了思考功能");
			}
		}

		return convertObjectToDocument(attributes);
	}

	public static Document convertObjectToDocument(Object value) {
		return switch (value) {
			case null -> Document.fromNull();
			case String stringValue -> Document.fromString(stringValue);
			case Boolean booleanValue -> Document.fromBoolean(booleanValue);
			case Integer integerValue -> Document.fromNumber(integerValue);
			case Long longValue -> Document.fromNumber(longValue);
			case Float floatValue -> Document.fromNumber(floatValue);
			case Double doubleValue -> Document.fromNumber(doubleValue);
			case BigDecimal bigDecimalValue -> Document.fromNumber(bigDecimalValue);
			case BigInteger bigIntegerValue -> Document.fromNumber(bigIntegerValue);
			case List listValue ->
					Document.fromList(listValue.stream().map(UranBedrockProxyChatModel::convertObjectToDocument).toList());
			case Map mapValue -> convertMapToDocument(mapValue);
			default ->
					throw new IllegalArgumentException(STR."Unsupported value type:\{value.getClass().getSimpleName()}");
		};
	}

	private static Document convertMapToDocument(Map<String, Object> value) {
		Map<String, Document> attr = value.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> convertObjectToDocument(e.getValue())));

		return Document.fromMap(attr);
	}

	private ContentBlock mapMediaToContentBlock(Media media) {

		var mimeType = media.getMimeType();

		if (BedrockMediaFormat.isSupportedVideoFormat(mimeType)) { // Video
			VideoFormat videoFormat = BedrockMediaFormat.getVideoFormat(mimeType);
			VideoSource videoSource = null;
			if (media.getData() instanceof byte[] bytes) {
				videoSource = VideoSource.builder().bytes(SdkBytes.fromByteArrayUnsafe(bytes)).build();
			} else if (media.getData() instanceof String uriText) {
				// if (URLValidator.isValidURLBasic(uriText)) {
				videoSource = VideoSource.builder().s3Location(S3Location.builder().uri(uriText).build()).build();
				// }
			} else if (media.getData() instanceof URL url) {
				try {
					videoSource = VideoSource.builder()
							.s3Location(S3Location.builder().uri(url.toURI().toString()).build())
							.build();
				} catch (URISyntaxException e) {
					throw new IllegalArgumentException(e);
				}
			} else {
				throw new IllegalArgumentException("Invalid video content type: " + media.getData().getClass());
			}

			return ContentBlock.fromVideo(VideoBlock.builder().source(videoSource).format(videoFormat).build());
		} else if (BedrockMediaFormat.isSupportedImageFormat(mimeType)) { // Image
			ImageSource.Builder sourceBuilder = ImageSource.builder();
			if (media.getData() instanceof byte[] bytes) {
				sourceBuilder.bytes(SdkBytes.fromByteArrayUnsafe(bytes)).build();
			} else if (media.getData() instanceof String text) {

				if (URLValidator.isValidURLBasic(text)) {
					try {
						URL url = new URL(text);
						URLConnection connection = url.openConnection();
						try (InputStream is = connection.getInputStream()) {
							sourceBuilder.bytes(SdkBytes.fromByteArrayUnsafe(StreamUtils.copyToByteArray(is))).build();
						}
					} catch (IOException e) {
						throw new RuntimeException("Failed to read media data from URL: " + text, e);
					}
				} else {
					sourceBuilder.bytes(SdkBytes.fromByteArray(Base64.getDecoder().decode(text)));
				}
			} else if (media.getData() instanceof URL url) {

				try (InputStream is = url.openConnection().getInputStream()) {
					byte[] imageBytes = StreamUtils.copyToByteArray(is);
					sourceBuilder.bytes(SdkBytes.fromByteArrayUnsafe(imageBytes)).build();
				} catch (IOException e) {
					throw new IllegalArgumentException("Failed to read media data from URL: " + url, e);
				}
			} else {
				throw new IllegalArgumentException("Invalid Image content type: " + media.getData().getClass());
			}

			return ContentBlock.fromImage(ImageBlock.builder()
					.source(sourceBuilder.build())
					.format(BedrockMediaFormat.getImageFormat(mimeType))
					.build());
		} else if (BedrockMediaFormat.isSupportedDocumentFormat(mimeType)) { // Document

			return ContentBlock.fromDocument(DocumentBlock.builder()
					.name(media.getName())
					.format(BedrockMediaFormat.getDocumentFormat(mimeType))
					.source(DocumentSource.builder().bytes(SdkBytes.fromByteArray(media.getDataAsByteArray())).build())
					.build());
		}

		throw new IllegalArgumentException("Unsupported media format: " + mimeType);
	}

	private static byte[] getContentMediaData(Object mediaData) {
		if (mediaData instanceof byte[] bytes) {
			return bytes;
		} else if (mediaData instanceof String text) {
			if (URLValidator.isValidURLBasic(text)) {
				try {
					URL url = new URL(text);
					URLConnection connection = url.openConnection();
					try (InputStream is = connection.getInputStream()) {
						return StreamUtils.copyToByteArray(is);
					}
				} catch (IOException e) {
					throw new RuntimeException("Failed to read media data from URL: " + text, e);
				}
			}
			return text.getBytes();
		} else if (mediaData instanceof URL url) {
			try (InputStream is = url.openConnection().getInputStream()) {
				return StreamUtils.copyToByteArray(is);
			} catch (IOException e) {
				throw new RuntimeException("Failed to read media data from URL: " + url, e);
			}
		} else {
			throw new IllegalArgumentException("Unsupported media data type: " + mediaData.getClass().getSimpleName());
		}
	}

	/**
	 * Convert {@link ConverseResponse} to {@link ChatResponse} includes model output,
	 * stopReason, usage, metrics etc.
	 * https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html#API_runtime_Converse_ResponseSyntax
	 *
	 * @param response The Bedrock Converse response.
	 * @return The ChatResponse entity.
	 */
	private ChatResponse toChatResponse(ConverseResponse response, ChatResponse perviousChatResponse) {

		Assert.notNull(response, "'response' must not be null.");

		Message message = response.output().message();

		List<Generation> generations = message.content()
				.stream()
				.filter(content -> content.type() != ContentBlock.Type.TOOL_USE)
				.map(content -> new Generation(new AssistantMessage(content.text(), Map.of()),
						ChatGenerationMetadata.builder().finishReason(response.stopReasonAsString()).build()))
				.toList();

		List<Generation> allGenerations = new ArrayList<>(generations);

		if (response.stopReasonAsString() != null && generations.isEmpty()) {
			Generation generation = new Generation(new AssistantMessage(null, Map.of()),
					ChatGenerationMetadata.builder().finishReason(response.stopReasonAsString()).build());
			allGenerations.add(generation);
		}

		List<ContentBlock> toolUseContentBlocks = message.content()
				.stream()
				.filter(c -> c.type() == ContentBlock.Type.TOOL_USE)
				.toList();

		if (!CollectionUtils.isEmpty(toolUseContentBlocks)) {

			List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

			for (ContentBlock toolUseContentBlock : toolUseContentBlocks) {

				var functionCallId = toolUseContentBlock.toolUse().toolUseId();
				var functionName = toolUseContentBlock.toolUse().name();
				var functionArguments = toolUseContentBlock.toolUse().input().toString();

				toolCalls
						.add(new AssistantMessage.ToolCall(functionCallId, "function", functionName, functionArguments));
			}

			AssistantMessage assistantMessage = new AssistantMessage("", Map.of(), toolCalls);
			Generation toolCallGeneration = new Generation(assistantMessage,
					ChatGenerationMetadata.builder().finishReason(response.stopReasonAsString()).build());
			allGenerations.add(toolCallGeneration);
		}

		Integer promptTokens = response.usage().inputTokens();
		Integer generationTokens = response.usage().outputTokens();
		int totalTokens = response.usage().totalTokens();

		if (perviousChatResponse != null && perviousChatResponse.getMetadata() != null
				&& perviousChatResponse.getMetadata().getUsage() != null) {

			promptTokens += perviousChatResponse.getMetadata().getUsage().getPromptTokens();
			generationTokens += perviousChatResponse.getMetadata().getUsage().getCompletionTokens();
			totalTokens += perviousChatResponse.getMetadata().getUsage().getTotalTokens();
		}

		DefaultUsage usage = new DefaultUsage(promptTokens, generationTokens, totalTokens);

		Document modelResponseFields = response.additionalModelResponseFields();

		ConverseMetrics metrics = response.metrics();

		var chatResponseMetaData = ChatResponseMetadata.builder()
				.id(response.responseMetadata() != null ? response.responseMetadata().requestId() : "Unknown")
				.usage(usage)
				.build();

		return new ChatResponse(allGenerations, chatResponseMetaData);
	}

	/**
	 * Invoke the model and return the response stream.
	 * <p>
	 * https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters.html
	 * https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html
	 * https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/bedrockruntime/BedrockRuntimeAsyncClient.html#converseStream
	 *
	 * @return The model invocation response stream.
	 */
	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {
		Prompt requestPrompt = buildRequestPrompt(prompt);
		return this.internalStream(requestPrompt, null);
	}

	private Flux<ChatResponse> internalStream(Prompt prompt, ChatResponse perviousChatResponse) {
		Assert.notNull(prompt, "'prompt' must not be null");
		String chatMemoryConversationId = getConversationId(prompt);
		return Flux.deferContextual(contextView -> {

			ConverseRequest converseRequest = this.createRequest(prompt);

			ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
					.prompt(prompt)
					.provider(AiProvider.BEDROCK_CONVERSE.value())
					.build();

			Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(
					this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
					this.observationRegistry);

			observation.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null)).start();

			ConverseStreamRequest converseStreamRequest = ConverseStreamRequest.builder()
					.modelId(converseRequest.modelId())
					.inferenceConfig(converseRequest.inferenceConfig())
					.messages(converseRequest.messages())
					.system(converseRequest.system())
					.additionalModelRequestFields(converseRequest.additionalModelRequestFields())
					.toolConfig(converseRequest.toolConfig())
					.build();

			Flux<ConverseStreamOutput> response = converseStream(converseStreamRequest);

			Flux<ChatResponse> chatResponses = UranConverseApiUtils.toChatResponse(response, perviousChatResponse);

			AtomicReference<String> reasoningContent = new AtomicReference<>(StrUtil.EMPTY);
			AtomicReference<String> signature = new AtomicReference<>(StrUtil.EMPTY);
			Flux<ChatResponse> chatResponseFlux = chatResponses.switchMap(chatResponse -> {
						if (ObjectUtil.isNotNull(chatResponse.getResult())) {
							ChatGenerationMetadata metadata = chatResponse.getResult().getMetadata();
							String curReasoningContent = metadata.getOrDefault(CommonConstants.REASONING_CONTENT, StrUtil.EMPTY);
							String curSignature = metadata.getOrDefault(CommonConstants.SIGNATURE, StrUtil.EMPTY);
							if (ObjectUtil.isNull(thinkingInfoMap.get(chatMemoryConversationId))) {
								thinkingInfoMap.put(chatMemoryConversationId, new HashMap<>());
							}
							if (StrUtil.isNotBlank(curReasoningContent)) {
								reasoningContent.set(reasoningContent + curReasoningContent);
								thinkingInfoMap.get(chatMemoryConversationId).put(CommonConstants.REASONING_CONTENT, reasoningContent.get());
							}

							if (StrUtil.isNotBlank(curSignature)) {
								signature.set(signature + curSignature);
								thinkingInfoMap.get(chatMemoryConversationId).put(CommonConstants.SIGNATURE, signature.get());
							}
						}

						if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(Objects.requireNonNull(prompt.getOptions()), chatResponse)
								&& chatResponse.hasFinishReasons(Set.of(StopReason.TOOL_USE.toString()))) {

							// FIXME: bounded elastic needs to be used since tool calling
							// is currently only synchronous
							return Flux.defer(() -> {
								var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, chatResponse);

								if (toolExecutionResult.returnDirect()) {
									// Return tool execution result directly to the client.
									return Flux.just(ChatResponse.builder()
											.from(chatResponse)
											.generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
											.build());
								} else {
									// Send the tool execution result back to the model.
									return this.internalStream(
											new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()),
											chatResponse);
								}
							}).subscribeOn(Schedulers.boundedElastic());
						} else {
							return Flux.just(chatResponse);
						}
					})// @formatter:off
					.doOnError(observation::error)
					.doFinally(s -> observation.stop())
					.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
			// @formatter:on

			return new MessageAggregator().aggregate(chatResponseFlux, observationContext::setResponse);
		});
	}

	private static String getConversationId(Prompt prompt) {
		Map<String, Object> userMetadata = prompt.getUserMessage().getMetadata();
		return userMetadata.containsKey(ChatMemory.CONVERSATION_ID) ? userMetadata.get(ChatMemory.CONVERSATION_ID).toString()
				: StrUtil.EMPTY;
	}

	public static final Sinks.EmitFailureHandler DEFAULT_EMIT_FAILURE_HANDLER = Sinks.EmitFailureHandler
			.busyLooping(Duration.ofSeconds(10));

	/**
	 * Invoke the model and return the response stream.
	 * <p>
	 * https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters.html
	 * https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html
	 * https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/bedrockruntime/BedrockRuntimeAsyncClient.html#converseStream
	 *
	 * @param converseStreamRequest Model invocation request.
	 * @return The model invocation response stream.
	 */
	public Flux<ConverseStreamOutput> converseStream(ConverseStreamRequest converseStreamRequest) {
		Assert.notNull(converseStreamRequest, "'converseStreamRequest' must not be null");

		Sinks.Many<ConverseStreamOutput> eventSink = Sinks.many().multicast().onBackpressureBuffer();

		ConverseStreamResponseHandler.Visitor visitor = ConverseStreamResponseHandler.Visitor.builder()
				.onDefault(output -> {
					logger.debug("Received converse stream output:{}", output);
					eventSink.emitNext(output, DEFAULT_EMIT_FAILURE_HANDLER);
				})
				.build();

		ConverseStreamResponseHandler responseHandler = ConverseStreamResponseHandler.builder()
				.onEventStream(stream -> stream.subscribe(e -> e.accept(visitor)))
				.onComplete(() -> {
					eventSink.emitComplete(DEFAULT_EMIT_FAILURE_HANDLER);
					logger.info("Completed streaming response.");
				})
				.onError(error -> {
					logger.error("Error handling Bedrock converse stream response", error);
					eventSink.emitError(error, DEFAULT_EMIT_FAILURE_HANDLER);
				})
				.build();

		this.bedrockRuntimeAsyncClient.converseStream(converseStreamRequest, responseHandler);

		return eventSink.asFlux();

	}

	/**
	 * Use the provided convention for reporting observation data
	 *
	 * @param observationConvention The provided convention
	 */
	public void setObservationConvention(ChatModelObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention cannot be null");
		this.observationConvention = observationConvention;
	}

	public static UranBedrockProxyChatModel.Builder builder() {
		return new UranBedrockProxyChatModel.Builder();
	}

	public static final class Builder {

		private AwsCredentialsProvider credentialsProvider;

		private Region region = Region.US_EAST_1;

		private Duration timeout = Duration.ofMinutes(10);

		private ToolCallingManager toolCallingManager;

		private ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();

		private ToolCallingChatOptions defaultOptions = ToolCallingChatOptions.builder().build();

		private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

		private ChatModelObservationConvention customObservationConvention;

		private BedrockRuntimeClient bedrockRuntimeClient;

		private BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient;

		private Builder() {
			try {
				region = DefaultAwsRegionProviderChain.builder().build().getRegion();
			} catch (SdkClientException e) {
				logger.warn("Failed to load region from DefaultAwsRegionProviderChain, using US_EAST_1", e);
			}
		}

		public UranBedrockProxyChatModel.Builder toolCallingManager(ToolCallingManager toolCallingManager) {
			this.toolCallingManager = toolCallingManager;
			return this;
		}

		public UranBedrockProxyChatModel.Builder toolExecutionEligibilityPredicate(
				ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
			this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
			return this;
		}

		public UranBedrockProxyChatModel.Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
			Assert.notNull(credentialsProvider, "'credentialsProvider' must not be null.");
			this.credentialsProvider = credentialsProvider;
			return this;
		}

		public UranBedrockProxyChatModel.Builder region(Region region) {
			Assert.notNull(region, "'region' must not be null.");
			this.region = region;
			return this;
		}

		public UranBedrockProxyChatModel.Builder timeout(Duration timeout) {
			Assert.notNull(timeout, "'timeout' must not be null.");
			this.timeout = timeout;
			return this;
		}

		public UranBedrockProxyChatModel.Builder defaultOptions(ToolCallingChatOptions defaultOptions) {
			Assert.notNull(defaultOptions, "'defaultOptions' must not be null.");
			this.defaultOptions = defaultOptions;
			return this;
		}

		public UranBedrockProxyChatModel.Builder observationRegistry(ObservationRegistry observationRegistry) {
			Assert.notNull(observationRegistry, "'observationRegistry' must not be null.");
			this.observationRegistry = observationRegistry;
			return this;
		}

		public UranBedrockProxyChatModel.Builder customObservationConvention(ChatModelObservationConvention observationConvention) {
			Assert.notNull(observationConvention, "'observationConvention' must not be null.");
			this.customObservationConvention = observationConvention;
			return this;
		}

		public UranBedrockProxyChatModel.Builder bedrockRuntimeClient(BedrockRuntimeClient bedrockRuntimeClient) {
			this.bedrockRuntimeClient = bedrockRuntimeClient;
			return this;
		}

		public UranBedrockProxyChatModel.Builder bedrockRuntimeAsyncClient(BedrockRuntimeAsyncClient bedrockRuntimeAsyncClient) {
			this.bedrockRuntimeAsyncClient = bedrockRuntimeAsyncClient;
			return this;
		}

		public UranBedrockProxyChatModel build() {

			if (this.bedrockRuntimeClient == null) {
				this.bedrockRuntimeClient = BedrockRuntimeClient.builder()
						.region(this.region)
						.httpClientBuilder(null)
						.credentialsProvider(this.credentialsProvider)
						.overrideConfiguration(c -> c.apiCallTimeout(this.timeout))
						.build();
			}

			if (this.bedrockRuntimeAsyncClient == null) {

				// TODO: Is it ok to configure the NettyNioAsyncHttpClient explicitly???
				var httpClientBuilder = NettyNioAsyncHttpClient.builder()
						.tcpKeepAlive(true)
						.connectionAcquisitionTimeout(Duration.ofSeconds(30))
						.maxConcurrency(200);

				var builder = BedrockRuntimeAsyncClient.builder()
						.region(this.region)
						.httpClientBuilder(httpClientBuilder)
						.credentialsProvider(this.credentialsProvider)
						.overrideConfiguration(c -> c.apiCallTimeout(this.timeout));
				this.bedrockRuntimeAsyncClient = builder.build();
			}

			UranBedrockProxyChatModel UranBedrockProxyChatModel = null;

			if (this.toolCallingManager != null) {
				UranBedrockProxyChatModel = new UranBedrockProxyChatModel(this.bedrockRuntimeClient,
						this.bedrockRuntimeAsyncClient, this.defaultOptions, this.observationRegistry,
						this.toolCallingManager, this.toolExecutionEligibilityPredicate);

			} else {
				UranBedrockProxyChatModel = new UranBedrockProxyChatModel(this.bedrockRuntimeClient,
						this.bedrockRuntimeAsyncClient, this.defaultOptions, this.observationRegistry,
						DEFAULT_TOOL_CALLING_MANAGER, this.toolExecutionEligibilityPredicate);
			}

			if (this.customObservationConvention != null) {
				UranBedrockProxyChatModel.setObservationConvention(this.customObservationConvention);
			}

			return UranBedrockProxyChatModel;
		}

	}

}