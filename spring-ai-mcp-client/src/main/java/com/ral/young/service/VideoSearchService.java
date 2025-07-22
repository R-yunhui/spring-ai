package com.ral.young.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ral.young.advisor.CustomMessageChatMemoryAdvisor;
import com.ral.young.vo.ChatRequestVO;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
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
import java.util.Map;

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
				.defaultAdvisors(CustomMessageChatMemoryAdvisor.builder(
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
				   - searchVideos：综合工具，整合了关键词检索、向量检索和精确筛选功能，一步完成视频检索过程，返回的结果可用于后续分析或报告生成
						
				2. 视频分析工具（必须由用户决策选择使用哪种工具）
				   - analyzeVideos：使用专业CV模型分析视频内容，检测特定事件或行为
				     * 精确度高但处理时间较长
				     * 可用于单视频分析或多视频批量分析
				     * 返回的分析结果可直接用于生成分析报告
				   - analyzeVideoBySlicing：使用大模型分析视频内容
				     * 处理速度较快但精确度可能略低
				     * 适用于复杂场景或需要快速结果的情况
				     * 返回的分析结果可直接用于生成分析报告
						
				3. 报告生成工具
				   - generateReport：多功能报告生成工具，支持以下场景：
				     * 检索报告：提供filteredVideos参数（来自searchVideos的结果）
				     * 分析报告：提供analysisResults参数（来自analyzeVideos或analyzeVideoBySlicing的结果）
				     * 综合报告：同时提供filteredVideos和analysisResults
				     * 独立主题报告：提供reportType参数，不需要视频相关参数
				     * 注意：当用户要求生成多份不同主题的报告时，需要多次调用此工具，每次生成一份报告
						
				工具调用规则：
						
				1. 在每次调用工具之前，简要说明你将执行的操作，例如："正在提取关键词以进行视频检索..."
						
				2. 在每次获得工具调用结果后，简要总结结果，例如："已找到3个相关视频，正在进行筛选..."
						
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
				   
				6. 在用户做出决策后，应立即执行相应的分析工具，并在分析完成后继续执行后续步骤（如生成报告）
						
				7. 当用户在一条消息中包含多个请求（如"检索→分析→报告"）时，应按顺序处理每个请求，在需要用户决策的环节暂停并等待用户输入
						
				处理常见场景的指南：
						
				1. 视频检索场景：查找符合条件的视频
				   - 先调用extractKeywords提取关键词
				   - 再调用searchVideos进行检索和筛选
				   - 检索结果应保存，可用于后续的视频分析或报告生成
						
				2. 多轮对话场景：记住上下文和之前的结果
				   - 如果用户先检索视频，后来要求分析这些视频，应使用之前的检索结果
				   - 如果用户先分析视频，后来要求生成报告，应使用之前的分析结果
				   - 确保在多轮对话中保持工作流的连续性，不要丢失之前的结果
						
				3. 报告生成场景：根据用户需求生成不同类型的报告
				   - 检索报告：基于视频检索结果生成（使用filteredVideos参数）
				   - 分析报告：基于视频分析结果生成（使用analysisResults参数）
				   - 综合报告：同时包含检索和分析信息（同时使用两个参数）
				   - 独立主题报告：不依赖检索或分析结果，直接生成特定主题报告
				   - 当用户要求在分析后生成报告时，必须将分析结果传递给generateReport工具
						
				4. 连续处理场景：当用户请求包含多个步骤时（如检索→分析→报告）
				   - 在用户选择分析方式后，应自动继续执行后续步骤，无需用户额外确认
				   - 如用户明确要求生成报告，在完成分析后应自动调用generateReport工具
				   - 保持各步骤之间的数据传递，确保分析结果正确传递给报告生成工具
						
				完整工作流示例：
						
				1. 检索并分析视频然后生成报告的完整流程：
				   extractKeywords → searchVideos → 询问用户选择分析方式 → analyzeVideos/analyzeVideoBySlicing → generateReport
				   
				   * 重要：在用户选择分析方式后，应自动完成后续的分析和报告生成步骤
						
				2. 直接分析指定视频然后生成报告的流程：
				   询问用户选择分析方式 → analyzeVideos/analyzeVideoBySlicing → generateReport
						
				保持回复简洁专业，突出重点信息。在工具调用过程中提供适当的进度更新，让用户了解当前处理状态。
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
				.toolContext(Map.of(ChatMemory.CONVERSATION_ID, chatRequestVO.getRequestId()))
				.options(DashScopeChatOptions.builder()
						.withModel(DashScopeApi.ChatModel.QWEN_PLUS.getModel())
						.withMaxToken(16384)
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