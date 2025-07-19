package com.ral.young;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author renyh
 * @description TODO
 * @date 2025/7/19 10:35
 * @since 1.0.0
 */
@SpringBootApplication(exclude = {
		org.springframework.ai.mcp.client.autoconfigure.SseHttpClientTransportAutoConfiguration.class,
})
@ComponentScan(basePackages = "org.springframework.ai.mcp.client")
@SuppressWarnings("preview")
public class StreamableMcpClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamableMcpClientApplication.class, args);
	}

	private final String userInput = "阿里巴巴西溪园区";

	@Bean
	public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools,
												 ConfigurableApplicationContext context) {

		return _ -> {

			var chatClient = chatClientBuilder
					.defaultToolCallbacks(tools)
					.build();

			System.out.println(STR."""
>>> QUESTION: \{userInput}""");

			System.out.println(STR."""
>>> ASSISTANT: \{chatClient.prompt(userInput).call().content()}""");

			System.out.println("\n>>> QUESTION: " + "黄金价格走势");
			System.out.println(STR."""
>>> ASSISTANT: \{chatClient.prompt("黄金价格走势").call().content()}""");

//            context.close();
		};
	}
}