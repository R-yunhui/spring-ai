package com.ral.young;

import com.ral.young.advisor.UranMessageChatMemoryAdvisor;
import com.ral.young.manager.CustomToolCallingManager;
import com.ral.young.memeory.impl.InMemoryRoundBasedChatMemoryRepository;
import com.ral.young.memeory.impl.UranChatMessageMemory;
import com.ral.young.model.UranBedrockProxyChatModel;
import com.ral.young.tools.AlgorithmAnalysisTools;
import com.ral.young.tools.AlgorithmMatchingTools;
import com.ral.young.tools.CapabilityGenerationTools;
import com.ral.young.tools.ReportGenerationTools;
import com.ral.young.tools.VideoAnalysisTools;
import com.ral.young.tools.VideoSearchTools;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.bedrock.autoconfigure.BedrockAwsConnectionProperties;
import org.springframework.ai.model.bedrock.converse.autoconfigure.BedrockConverseProxyChatProperties;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/**
 * @author renyh
 * @description mcp client application
 * @date 2025/6/30 10:13
 * @since 1.0.0
 */
@SpringBootApplication
public class McpClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpClientApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider customToolCallbackProvider(ApplicationContext applicationContext) {
		VideoSearchTools videoSearchTools = applicationContext.getBean(VideoSearchTools.class);
		VideoAnalysisTools videoAnalysisTools = applicationContext.getBean(VideoAnalysisTools.class);
		ReportGenerationTools generationTools = applicationContext.getBean(ReportGenerationTools.class);
		AlgorithmAnalysisTools algorithmAnalysisTools = applicationContext.getBean(AlgorithmAnalysisTools.class);
		AlgorithmMatchingTools algorithmMatchingTools = applicationContext.getBean(AlgorithmMatchingTools.class);
		CapabilityGenerationTools capabilityGenerationTools = applicationContext.getBean(CapabilityGenerationTools.class);
		return MethodToolCallbackProvider.builder()
				.toolObjects(videoSearchTools, videoAnalysisTools, generationTools
						, algorithmAnalysisTools, algorithmMatchingTools, capabilityGenerationTools)
				.build();
	}

	@Bean
	public UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor() {
		return UranMessageChatMemoryAdvisor.builder(
						UranChatMessageMemory.builder()
								.chatMemoryRepository(
										new InMemoryRoundBasedChatMemoryRepository())
								.maxMessages(50)
								.build())
				.build();
	}

	@Bean
	public ToolCallingManager toolCallingManager(ToolCallbackResolver toolCallbackResolver,
												 ToolExecutionExceptionProcessor toolExecutionExceptionProcessor,
												 ObjectProvider<ObservationRegistry> observationRegistry,
												 ObjectProvider<ToolCallingObservationConvention> observationConvention,
												 UranMessageChatMemoryAdvisor uranMessageChatMemoryAdvisor
	) {
		var toolCallingManager = CustomToolCallingManager.builder()
				.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
				.toolCallbackResolver(toolCallbackResolver)
				.toolExecutionExceptionProcessor(toolExecutionExceptionProcessor)
				.messageChatMemoryAdvisor(uranMessageChatMemoryAdvisor)
				.build();

		observationConvention.ifAvailable(toolCallingManager::setObservationConvention);

		return toolCallingManager;
	}

	@Bean
	public UranBedrockProxyChatModel uranBedrockProxyChatModel(AwsCredentialsProvider credentialsProvider,
															   AwsRegionProvider regionProvider, BedrockAwsConnectionProperties connectionProperties,
															   BedrockConverseProxyChatProperties chatProperties, ToolCallingManager toolCallingManager,
															   ObjectProvider<ObservationRegistry> observationRegistry,
															   ObjectProvider<ChatModelObservationConvention> observationConvention,
															   ObjectProvider<BedrockRuntimeClient> bedrockRuntimeClient,
															   ObjectProvider<BedrockRuntimeAsyncClient> bedrockRuntimeAsyncClient,
															   ObjectProvider<ToolExecutionEligibilityPredicate> bedrockToolExecutionEligibilityPredicate) {

		var chatModel = UranBedrockProxyChatModel.builder()
				.credentialsProvider(credentialsProvider)
				.region(regionProvider.getRegion())
				.timeout(connectionProperties.getTimeout())
				.defaultOptions(chatProperties.getOptions())
				.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
				.toolCallingManager(toolCallingManager)
				.toolExecutionEligibilityPredicate(
						bedrockToolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
				.bedrockRuntimeClient(bedrockRuntimeClient.getIfAvailable())
				.bedrockRuntimeAsyncClient(bedrockRuntimeAsyncClient.getIfAvailable())
				.build();

		observationConvention.ifAvailable(chatModel::setObservationConvention);

		return chatModel;
	}
}