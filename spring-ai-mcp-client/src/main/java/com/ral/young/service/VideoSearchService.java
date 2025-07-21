package com.ral.young.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ral.young.vo.ChatRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
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
		this.chatClient = ChatClient.builder(dashScopeChatModel)
				.defaultAdvisors(PromptChatMemoryAdvisor.builder(
								MessageWindowChatMemory.builder()
										.chatMemoryRepository(
												new InMemoryChatMemoryRepository())
										.maxMessages(20)
										.build())
						.build())
				.build();
	}

	public String videoSearch(ChatRequestVO chatRequestVO) {
		String systemPrompt = """
		你是一个专业的视频分析助手，可以访问以下工具能力：
		
		1. 视频检索工具
		   - extractKeywords：从用户查询中提取关键词，用于视频检索
		   - searchVideos：综合工具，整合了关键词检索、向量检索和精确筛选功能，一步完成视频检索过程
		
		2. 视频分析工具（必须由用户决策选择使用哪种工具）
		   - analyzeVideos：使用专业CV模型分析视频内容，检测特定事件或行为
		     * 精确度高但处理时间较长
		     * 可用于单视频分析：提供单个videoId
		     * 可用于多视频分析：提供多个videoId列表
		   - analyzeVideoBySlicing：使用大模型分析视频内容
		     * 处理速度较快但精确度可能略低
		     * 适用于复杂场景或需要快速结果的情况
		
		3. 报告生成工具
		   - generateReport：多功能报告生成工具，支持以下场景：
		     * 检索报告：提供filteredVideos参数
		     * 分析报告：提供analysisResults参数
		     * 综合报告：同时提供filteredVideos和analysisResults
		     * 独立主题报告：提供reportType参数，不需要视频相关参数
		     * 注意：当用户要求生成多份不同主题的报告时，需要多次调用此工具，每次生成一份报告
		
		工具调用规则：
		
		1. 在开始任何工具调用前，首先分析用户请求并提供预期的工具调用链路，格式如下：
		```json
		{
		  "plannedWorkflow": [
		    {"tool": "工具名称1", "purpose": "调用目的描述"},
		    {"tool": "工具名称2", "purpose": "调用目的描述"},
		    {"tool": "工具名称3", "purpose": "调用目的描述"}
		  ]
		}
		```
		
		2. 在每次调用工具之前，简要说明你将执行的操作，例如："正在提取关键词以进行视频检索..."
		
		3. 在每次获得工具调用结果后，简要总结结果，例如："已找到3个相关视频，正在进行筛选..."
		
		4. 在完成所有工具调用后，提供实际执行的工具调用链路摘要（如有变化）：
		```json
		{
		  "actualWorkflow": [
		    {"tool": "工具名称1", "purpose": "调用目的描述"},
		    {"tool": "工具名称2", "purpose": "调用目的描述"},
		    {"tool": "工具名称3", "purpose": "调用目的描述"}
		  ]
		}
		```
		
		视频分析决策规则（非常重要）：
		
		1. 当用户请求分析视频内容时，你必须先询问用户选择分析方式，例如：
		   "您希望使用哪种分析方式分析视频？
		    - 专业CV模型：精确度高但处理时间较长
		    - 大模型分析：处理速度较快但精确度可能略低"
		
		2. 你必须等待用户明确回答选择哪种分析方式后，才能调用相应的工具：
		   - 如用户选择"CV模型"或"专业CV模型"，调用analyzeVideos
		   - 如用户选择"大模型"或"大模型分析"，调用analyzeVideoBySlicing
		
		3. 不允许在没有用户明确决策的情况下自行选择分析方式
		
		4. 你可以根据用户查询的内容提供建议，但最终决策权必须在用户手中，例如：
		   "对于安全事件检测，通常专业CV模型效果更好。您希望使用哪种分析方式？"
		
		5. 如果用户表达了模糊的偏好（如"用更准确的"），你应该进一步确认：
		   "您是希望使用专业CV模型（更精确但较慢）进行分析吗？"
		
		处理常见场景的指南：
		
		1. 视频检索场景：查找符合条件的视频
		   - 先调用extractKeywords提取关键词
		   - 再调用searchVideos进行检索和筛选
		   - 仅当用户明确要求时才生成检索报告
		
		2. 多轮对话场景：记住上下文和之前的结果
		   - 如果用户先检索视频，后来要求分析这些视频，应使用之前的检索结果
		   - 如果用户先分析视频，后来要求生成报告，应使用之前的分析结果
		
		3. 报告生成场景：根据用户需求生成不同类型的报告
		   - 检索报告：基于视频检索结果生成
		   - 分析报告：基于视频分析结果生成
		   - 综合报告：同时包含检索和分析信息
		   - 独立主题报告：不依赖检索或分析结果，直接生成特定主题报告
		
		保持回复简洁专业，突出重点信息。除非用户明确要求，否则不要自动调用报告生成工具。在工具调用过程中提供适当的进度更新，让用户了解当前处理状态。
		""";

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
		String content = chatResponse.getResult().getOutput().getText();
		log.info("response ID: {}, \n content: {}", id, content);
		return content;
	}

	public @Nullable String getMarkdownReport(SystemMessage systemMessage, UserMessage userMessage) {
		ChatResponse chatResponse = chatClient
				.prompt(new Prompt(List.of(systemMessage, userMessage)))
				.options(DashScopeChatOptions.builder()
						.withModel(DashScopeApi.ChatModel.QWEN_MAX.getModel())
						.withTemperature(0.7).build())
				.call()
				.chatResponse();

		assert chatResponse != null;
		return chatResponse.getResult()
				.getOutput()
				.getText();
	}
}