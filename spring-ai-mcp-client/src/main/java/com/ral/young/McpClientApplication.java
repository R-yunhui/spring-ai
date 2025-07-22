package com.ral.young;

import com.ral.young.advisor.CustomMessageChatMemoryAdvisor;
import com.ral.young.manager.CustomToolCallingManager;
import com.ral.young.tools.ReportGenerationTools;
import com.ral.young.tools.VideoAnalysisTools;
import com.ral.young.tools.VideoSearchTools;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
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
		return MethodToolCallbackProvider.builder()
				.toolObjects(videoSearchTools, videoAnalysisTools, generationTools)
				.build();
	}

	@Bean
	public CustomMessageChatMemoryAdvisor customMessageChatMemoryAdvisor() {
		return CustomMessageChatMemoryAdvisor.builder(
						MessageWindowChatMemory.builder()
								.chatMemoryRepository(
										new InMemoryChatMemoryRepository())
								.maxMessages(20)
								.build())
				.build();
	}

	@Bean
	ToolCallingManager toolCallingManager(ToolCallbackResolver toolCallbackResolver,
										  ToolExecutionExceptionProcessor toolExecutionExceptionProcessor,
										  ObjectProvider<ObservationRegistry> observationRegistry,
										  ObjectProvider<ToolCallingObservationConvention> observationConvention,
										  CustomMessageChatMemoryAdvisor customMessageChatMemoryAdvisor
										  ) {
		var toolCallingManager = CustomToolCallingManager.builder()
				.observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
				.toolCallbackResolver(toolCallbackResolver)
				.toolExecutionExceptionProcessor(toolExecutionExceptionProcessor)
				.messageChatMemoryAdvisor(customMessageChatMemoryAdvisor)
				.build();

		observationConvention.ifAvailable(toolCallingManager::setObservationConvention);

		return toolCallingManager;
	}
}