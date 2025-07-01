package com.ral.young.service;

import com.ral.young.vo.ChatRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * @author renyh
 * @description mcp 调用
 * @date 2025/6/30 19:32
 * @since 1.0.0
 */
@Service
@Slf4j
public class McpService {

	private final ChatClient chatClient;

	private final ToolCallbackProvider tools;

	public McpService(ChatModel dashScopeChatModel, ToolCallbackProvider tools) {
		this.tools = tools;
		this.chatClient = ChatClient.builder(dashScopeChatModel)
				.defaultSystem("你是一个助手，回答问题的同时，保持语言的简洁和专业。回复的结果控制在200字左右。")
				.defaultToolCallbacks(tools)
				.defaultAdvisors(
						MessageChatMemoryAdvisor.builder(
										MessageWindowChatMemory.builder()
												.chatMemoryRepository(new InMemoryChatMemoryRepository())
												.maxMessages(10)
												.build()
								)
								.build()
				)
				.build();
	}

	public String chatWithMcp(ChatRequestVO chatRequestVO) {
		ChatResponse chatResponse = chatClient.prompt(chatRequestVO.getPrompt())
				.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatRequestVO.getRequestId()))
				.call()
				.chatResponse();
		assert chatResponse != null;
		return chatResponse.getResult()
				.getOutput()
				.getText();
	}

	public String getMcpTools() {
		ToolCallback[] toolCallbacks = tools.getToolCallbacks();
		log.info("tool 数量: {}", toolCallbacks.length);
		List<String> toolNames = Arrays.stream(toolCallbacks).map(toolCallback -> toolCallback.getToolDefinition().name()).toList();
		return String.join("\n", toolNames);
	}
}
