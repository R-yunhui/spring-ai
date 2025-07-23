package com.ral.young.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ral.young.common.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.api.ConverseApiUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDelta;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDeltaEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockStartEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockStopEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamMetadataEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamMetrics;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamOutput;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStartEvent;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStopEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author renyh
 * @description 自定义 ConverseApiUtils
 * @date 2025/7/25 9:30
 * @since 1.0.0
 */
public class UranConverseApiUtils {

	private static final Logger log = LoggerFactory.getLogger(UranConverseApiUtils.class);

	public static Flux<ChatResponse> toChatResponse(Flux<ConverseStreamOutput> responses,
													ChatResponse perviousChatResponse) {

		AtomicBoolean isInsideTool = new AtomicBoolean(false);

		return responses.mapNotNull(event -> {
					if (ConverseApiUtils.isToolUseStart(event)) {
						isInsideTool.set(true);
					}
					return event;
				}).windowUntil(event -> { // Group all chunks belonging to the same function call.
					if (isInsideTool.get() && ConverseApiUtils.isToolUseFinish(event)) {
						isInsideTool.set(false);
						return true;
					}
					return !isInsideTool.get();
				}).concatMapIterable(window -> { // Merging the window chunks into a single chunk.
					Mono<ConverseStreamOutput> monoChunk = window.reduce(new ConverseApiUtils.ToolUseAggregationEvent(),
							ConverseApiUtils::mergeToolUseEvents);
					return List.of(monoChunk);
				}).flatMap(mono -> mono).scanWith(ConverseApiUtils.Aggregation::new, (lastAggregation, nextEvent) -> {

					// System.out.println(nextEvent);
					if (nextEvent instanceof ConverseApiUtils.ToolUseAggregationEvent toolUseAggregationEvent) {

						if (CollectionUtils.isEmpty(toolUseAggregationEvent.toolUseEntries())) {
							return new ConverseApiUtils.Aggregation();
						}

						List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

						Integer promptTokens = 0;
						Integer generationTokens = 0;
						Integer totalTokens = 0;

						for (ConverseApiUtils.ToolUseAggregationEvent.ToolUseEntry toolUseEntry : toolUseAggregationEvent.toolUseEntries()) {
							var functionCallId = toolUseEntry.id();
							var functionName = toolUseEntry.name();
							var functionArguments = toolUseEntry.input();
							toolCalls.add(
									new AssistantMessage.ToolCall(functionCallId, "function", functionName, functionArguments));

							if (toolUseEntry.usage() != null) {
								promptTokens += toolUseEntry.usage().getPromptTokens();
								generationTokens += toolUseEntry.usage().getCompletionTokens();
								totalTokens += toolUseEntry.usage().getTotalTokens();
							}
						}

						AssistantMessage assistantMessage = new AssistantMessage("", Map.of(), toolCalls);
						Generation toolCallGeneration = new Generation(assistantMessage,
								ChatGenerationMetadata.builder().finishReason("tool_use").build());

						var chatResponseMetaData = ChatResponseMetadata.builder()
								.usage(new DefaultUsage(promptTokens, generationTokens, totalTokens))
								.build();

						return new ConverseApiUtils.Aggregation(
								ConverseApiUtils.MetadataAggregation.builder().copy(lastAggregation.metadataAggregation()).build(),
								new ChatResponse(List.of(toolCallGeneration), chatResponseMetaData));

					} else if (nextEvent instanceof MessageStartEvent messageStartEvent) {
						var newMeta = ConverseApiUtils.MetadataAggregation.builder()
								.copy(lastAggregation.metadataAggregation())
								.withRole(messageStartEvent.role().toString())
								.build();
						return new ConverseApiUtils.Aggregation(newMeta, ConverseApiUtils.EMPTY_CHAT_RESPONSE);
					} else if (nextEvent instanceof MessageStopEvent messageStopEvent) {
						var newMeta = ConverseApiUtils.MetadataAggregation.builder()
								.copy(lastAggregation.metadataAggregation())
								.withStopReason(messageStopEvent.stopReasonAsString())
								.withAdditionalModelResponseFields(messageStopEvent.additionalModelResponseFields())
								.build();
						return new ConverseApiUtils.Aggregation(newMeta, ConverseApiUtils.EMPTY_CHAT_RESPONSE);
					} else if (nextEvent instanceof ContentBlockStartEvent contentBlockStartEvent) {
						// TODO ToolUse support
						return new ConverseApiUtils.Aggregation();
					} else if (nextEvent instanceof ContentBlockDeltaEvent contentBlockDeltaEvent) {
						if (contentBlockDeltaEvent.delta().type().equals(ContentBlockDelta.Type.TEXT) || contentBlockDeltaEvent.delta().type().equals(ContentBlockDelta.Type.REASONING_CONTENT)) {

							Map<String, Object> metadataMap = new HashMap<>(4);
							if (ObjectUtil.isNotNull(contentBlockDeltaEvent.delta().reasoningContent())) {
								if (StrUtil.isNotBlank(contentBlockDeltaEvent.delta().reasoningContent().text())) {
									metadataMap.put(CommonConstants.REASONING_CONTENT, contentBlockDeltaEvent.delta().reasoningContent().text());
								}

								if (StrUtil.isNotBlank(contentBlockDeltaEvent.delta().reasoningContent().signature())) {
									metadataMap.put(CommonConstants.SIGNATURE, contentBlockDeltaEvent.delta().reasoningContent().signature());
								}
							}

							var generation = new Generation(
									new AssistantMessage(contentBlockDeltaEvent.delta().text(), Map.of()),
									ChatGenerationMetadata.builder()
											.finishReason(lastAggregation.metadataAggregation().stopReason())
											.metadata(metadataMap)
											.build());

							return new ConverseApiUtils.Aggregation(
									ConverseApiUtils.MetadataAggregation.builder().copy(lastAggregation.metadataAggregation()).build(),
									new ChatResponse(List.of(generation)));
						} else if (contentBlockDeltaEvent.delta().type().equals(ContentBlockDelta.Type.TOOL_USE)) {
							// TODO ToolUse support
						}
						return new ConverseApiUtils.Aggregation();
					} else if (nextEvent instanceof ContentBlockStopEvent contentBlockStopEvent) {
						// TODO ToolUse support
						return new ConverseApiUtils.Aggregation();
					} else if (nextEvent instanceof ConverseStreamMetadataEvent metadataEvent) {

						var newMeta = ConverseApiUtils.MetadataAggregation.builder()
								.copy(lastAggregation.metadataAggregation())
								.withTokenUsage(metadataEvent.usage())
								.withMetrics(metadataEvent.metrics())
								.withTrace(metadataEvent.trace())
								.build();

						// TODO
						Document modelResponseFields = lastAggregation.metadataAggregation().additionalModelResponseFields();
						ConverseStreamMetrics metrics = metadataEvent.metrics();

						DefaultUsage usage = new DefaultUsage(metadataEvent.usage().inputTokens(),
								metadataEvent.usage().outputTokens(), metadataEvent.usage().totalTokens());

						var chatResponseMetaData = ChatResponseMetadata.builder().usage(usage).build();

						return new ConverseApiUtils.Aggregation(newMeta, new ChatResponse(List.of(), chatResponseMetaData));
					} else {
						return new ConverseApiUtils.Aggregation();
					}
				})
				// .skip(1)
				.filter(aggregation -> aggregation.chatResponse() != ConverseApiUtils.EMPTY_CHAT_RESPONSE)
				.map(aggregation -> {

					var chatResponse = aggregation.chatResponse();

					// Merge the previous chat response metadata with the current one.
					if (perviousChatResponse != null && perviousChatResponse.getMetadata() != null
							&& perviousChatResponse.getMetadata().getUsage() != null) {

						var metadataBuilder = ChatResponseMetadata.builder();

						Integer promptTokens = perviousChatResponse.getMetadata().getUsage().getPromptTokens();
						Integer generationTokens = perviousChatResponse.getMetadata().getUsage().getCompletionTokens();
						int totalTokens = perviousChatResponse.getMetadata().getUsage().getTotalTokens();

						if (chatResponse.getMetadata() != null) {
							metadataBuilder.id(chatResponse.getMetadata().getId());
							metadataBuilder.model(chatResponse.getMetadata().getModel());
							metadataBuilder.rateLimit(chatResponse.getMetadata().getRateLimit());
							metadataBuilder.promptMetadata(chatResponse.getMetadata().getPromptMetadata());

							if (chatResponse.getMetadata().getUsage() != null) {
								promptTokens = promptTokens + chatResponse.getMetadata().getUsage().getPromptTokens();
								generationTokens = generationTokens
										+ chatResponse.getMetadata().getUsage().getCompletionTokens();
								totalTokens = totalTokens + chatResponse.getMetadata().getUsage().getTotalTokens();
							}
						}

						metadataBuilder.usage(new DefaultUsage(promptTokens, generationTokens, totalTokens));

						return new ChatResponse(chatResponse.getResults(), metadataBuilder.build());
					}

					return aggregation.chatResponse();
				});
	}
}
