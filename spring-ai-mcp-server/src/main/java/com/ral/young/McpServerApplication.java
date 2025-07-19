package com.ral.young;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author renyh
 * @description mcp server application
 * @date 2025/6/30 10:13
 * @since 1.0.0
 */
@SpringBootApplication
public class McpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider weatherTools(OpenMeteoService openMeteoService) {
		return MethodToolCallbackProvider.builder().toolObjects(openMeteoService).build();
	}

	public record TextInput(String input) {
	}

	/**
	 * 将输入字母转为大写的 MCP Tools
	 */
	@Bean
	public ToolCallback toUpperCase() {
		return FunctionToolCallback.builder("toUpperCase", (TextInput input) -> input.input().toUpperCase())
				.inputType(TextInput.class)
				.description("Put the text to upper case")
				.build();
	}
}