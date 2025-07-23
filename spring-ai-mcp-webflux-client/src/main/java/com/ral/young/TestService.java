package com.ral.young;

import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author renyh
 * @description TODO
 * @date 2025/7/23 20:15
 * @since 1.0.0
 */
@Service
@SuppressWarnings("preview")
public class TestService {

	private final ChatClient.Builder chatClientBuilder;

	private final ToolCallbackProvider tools;

	public TestService(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
		this.chatClientBuilder = chatClientBuilder;
		this.tools = tools;
	}

	@Scheduled(fixedDelay = 10000)
	public void test() {
		for (int i = 0; i < 5; i++) {
			try {
				var chatClient = chatClientBuilder.defaultToolCallbacks(tools).build();

				String userInput1 = "北京的天气和空气质量如何？";
				System.out.println(STR."""
>>> QUESTION: \{userInput1}""");

				System.out.println(STR."""
>>> ASSISTANT: \{chatClient.prompt(userInput1).call().content()}""");

				String userInput2 = "将 user 转为大写";
				System.out.println(STR."""
>>> QUESTION: \{userInput2}""");

				System.out.println(STR."""
>>> ASSISTANT: \{chatClient.prompt(userInput2).call().content()}""");

				Thread.sleep(30000);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}
}
