package com.ral.young.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
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
import org.springframework.ai.tool.definition.ToolDefinition;
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
@SuppressWarnings("preview")
public class McpService {

	private final ChatClient chatClient;

	private final List<ToolCallbackProvider> tools;

	public McpService(DashScopeChatModel dashScopeChatModel, List<ToolCallbackProvider> tools) {
		this.tools = tools;
		List<ToolCallback> toolCallbacks = tools.stream()
				.map(ToolCallbackProvider::getToolCallbacks)  // 获取每个 ToolCallbackProvider 的回调数组
				.flatMap(Arrays::stream)  // 将多个数组合并为一个流
				.toList();
		this.chatClient = ChatClient.builder(dashScopeChatModel)
				.defaultSystem("你是一个助手，回答问题的同时，保持语言的简洁和专业。回复的结果控制在200字左右。")
				.defaultToolCallbacks(toolCallbacks)
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
				.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatRequestVO.getConversationId()))
				.call()
				.chatResponse();
		assert chatResponse != null;
		return chatResponse.getResult()
				.getOutput()
				.getText();
	}

	public String getMcpTools() {
		List<ToolCallback> toolCallbacks = tools.stream()
				.map(ToolCallbackProvider::getToolCallbacks)  // 获取每个 ToolCallbackProvider 的回调数组
				.flatMap(Arrays::stream)  // 将多个数组合并为一个流
				.toList();
		log.info("tool 数量: {}", toolCallbacks.size());
		// 打印 tool 定义
		List<ToolDefinition> definitions = toolCallbacks.stream()
				.map(ToolCallback::getToolDefinition)
				.toList();
		List<JSONObject> defineList = definitions.stream().map(definition ->
				{
					JSONObject obj = new JSONObject();
					obj.set("name", definition.name());
					obj.set("description", definition.description());
					obj.set("inputSchema", definition.inputSchema());
					return obj;
				}
		).toList();
		String result = JSONUtil.toJsonPrettyStr(defineList);
		log.info("工具信息: {}", result);
		return result;
	}
}
