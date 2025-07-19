package com.ral.young;

import com.ral.young.tools.VideoSearchTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
	public ToolCallbackProvider weatherTools(VideoSearchTools videoSearchTools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(videoSearchTools)
				.build();
	}
}