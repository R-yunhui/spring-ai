package com.ral.young.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ral.young.vo.ChatRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * @author renyh
 * @description 视频检索demo
 * @date 2025/7/19 15:08
 * @since 1.0.0
 */
@Service
@Slf4j
public class VideoSearchService {

	private final ChatClient chatClient;
	private final List<ToolCallbackProvider> tools;

	public VideoSearchService(ChatModel dashScopeChatModel, List<ToolCallbackProvider> tools) {
		this.tools = tools;
		this.chatClient = ChatClient.builder(dashScopeChatModel).defaultAdvisors(PromptChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().chatMemoryRepository(new InMemoryChatMemoryRepository()).maxMessages(20).build()).build()).build();
	}

	public String videoSearch(ChatRequestVO chatRequestVO) {
		String systemPrompt = """
         你是一个专业的视频检索和报告生成助手。你需要严格按照以下场景处理用户请求：

         1. 视频检索场景：当用户仅需要查找视频时（例如："查找穿着黑色上衣的人"）
            - 按顺序调用工具：extractKeywords → searchVideosByKeywords/searchVideosByEmbedding → filterVideoResults
            - 不要调用generateSearchReport工具
           \s
         2. 视频检索并生成报告：当用户明确要求生成报告时（例如："查找视频并生成报告"、"为我的检索结果生成报告"）
            - 按顺序调用工具：extractKeywords → searchVideosByKeywords/searchVideosByEmbedding → filterVideoResults → generateSearchReport
            - 为generateSearchReport提供filteredVideos参数，不需要提供reportType参数
           \s
         3. 独立生成报告：当用户需要生成与视频检索无关的报告时（例如："生成关于某主题的报告"）
            - 直接使用generateSearchReport工具
            - 不提供filteredVideos参数
            - 根据用户意图提供适当的reportType参数
           \s
         请仔细分析用户的请求内容，准确判断用户意图并选择合适的处理流程。除非用户明确要求生成报告，否则不要自动调用报告生成工具。保持回复简洁专业。
       \s""";

		List<ToolCallback> toolCallbacks = tools.stream()
				.map(ToolCallbackProvider::getToolCallbacks)
				.flatMap(Arrays::stream)
				.toList();

		// 获取用户查询
		String userQuery = chatRequestVO.getPrompt();

		// 构建用户提示
		SystemMessage systemMessage = new SystemMessage(systemPrompt);
		UserMessage userMessage = new UserMessage(userQuery);

		ChatResponse chatResponse = chatClient
				.prompt(new Prompt(List.of(systemMessage, userMessage)))
				.options(DashScopeChatOptions.builder()
						.withModel(DashScopeApi.ChatModel.QWEN_MAX.getModel())
						.withTemperature(0.7).build())
				.advisors(advisor ->
						advisor.param(ChatMemory.CONVERSATION_ID, chatRequestVO.getRequestId())
				)
				.toolCallbacks(toolCallbacks)
				.call()
				.chatResponse();
		assert chatResponse != null;
		String id = chatResponse.getMetadata().getId();
		log.info("response ID：{}", id);
		return chatResponse.getResult().getOutput().getText();
	}
}