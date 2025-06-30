package com.ral.young;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

/**
 * @author renyh
 * @description mcp client application
 * @date 2025/6/30 10:13
 * @since 1.0.0
 */
@SpringBootApplication
@SuppressWarnings("preview")
public class McpClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpClientApplication.class, args);
	}

	private final String userInput = """
						 提示：
						 你是一个数据分析师，你需要根据用户的问题，回答用户的问题， 当前数据库里面的 event_detail 表里面记录了所有的告警事件，你需要根据 event_detail 表里面的告警事件，回答用户的问题。
						 \s
						 1.我想知道最近半年产生了多少告警事件？
						 2.分别都是什么类型的告警事件？
						 3.哪种类型的告警事件最多，最少，各占多少，占比多少？
						""";

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

			context.close();
		};
	}
}