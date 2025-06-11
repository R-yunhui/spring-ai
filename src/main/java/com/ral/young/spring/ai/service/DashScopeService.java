package com.ral.young.spring.ai.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

/**
 * @author renyh
 * @description 阿里巴巴模型接口
 * @date 2025/6/10 14:15
 * @since 1.0.0
 */
@Service
public class DashScopeService {

	private final ChatClient dashScopeChatClient;

	private final ToolService toolService;

	public DashScopeService(DashScopeChatModel dashScopeChatModel, ToolService toolService) {
		this.dashScopeChatClient = ChatClient.builder(dashScopeChatModel)
				// 实现 Logger 的 Advisor
				.defaultAdvisors(
						new SimpleLoggerAdvisor()
				)
				// 设置 ChatClient 中 ChatModel 的 Options 参数
				.defaultOptions(
						DashScopeChatOptions.builder()
								.withTopP(0.7)
								.build())
				.build();
		this.toolService = toolService;
	}

	public String testTool(String prompt) {
		ChatResponse chatResponse = dashScopeChatClient.prompt(prompt)
				.tools(toolService)
				.call()
				.chatResponse();
		assert chatResponse != null;
		return chatResponse.getResult()
				.getOutput()
				.getText();
	}
}
