package com.ral.young;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author renyh
 * @description 通过 webflux 的方式调用 mcp server
 * @date 2025/7/19 10:24
 * @since 1.0.0
 */
@SpringBootApplication(exclude =
		{org.springframework.ai.mcp.client.autoconfigure.SseHttpClientTransportAutoConfiguration.class}
)
@SuppressWarnings("preview")
public class WebFluxMcpClientApplication {

	public static void main(String[] args) {
		System.out.println("Hello world!");
		SpringApplication.run(WebFluxMcpClientApplication.class, args);
	}

	private final String userInput1 = "北京的天气和空气质量如何？";

	private final String userInput2 = "将 user 转为大写";

	@Bean
	public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools, ConfigurableApplicationContext context) {

		return _ -> {

			var chatClient = chatClientBuilder.defaultToolCallbacks(tools).build();

			System.out.println(STR."""
>>> QUESTION: \{userInput1}""");

			System.out.println(STR."""
>>> ASSISTANT: \{chatClient.prompt(userInput1).call().content()}""");

			System.out.println(STR."""
>>> QUESTION: \{userInput2}""");

			System.out.println(STR."""
>>> ASSISTANT: \{chatClient.prompt(userInput2).call().content()}""");

			context.close();
		};
	}
}