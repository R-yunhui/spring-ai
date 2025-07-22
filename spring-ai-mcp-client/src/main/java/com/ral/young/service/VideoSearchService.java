package com.ral.young.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ral.young.advisor.CustomMessageChatMemoryAdvisor;
import com.ral.young.vo.ChatRequestVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.BaseChatMemoryAdvisor;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

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

	public final CustomMessageChatMemoryAdvisor customMessageChatMemoryAdvisor;

	public VideoSearchService(ChatModel dashScopeChatModel, List<ToolCallbackProvider> tools, CustomMessageChatMemoryAdvisor customMessageChatMemoryAdvisor) {
		this.tools = tools;
		this.customMessageChatMemoryAdvisor = customMessageChatMemoryAdvisor;
		this.chatClient = ChatClient.builder(dashScopeChatModel)
				.defaultAdvisors(customMessageChatMemoryAdvisor)
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
				
		工具调用标记规则（非常重要）：
				
		1. 在每个处理阶段，必须提供清晰的开始和结束标记，格式如下：
		   - [START] 简要描述当前阶段的操作或需要用户做的决策 [DONE]
				
		2. 标记必须放在独立行，确保前端能正确解析
				
		3. 每个工具调用和处理阶段都必须有对应的标记，这对前端展示至关重要
		
		4. 区分自动执行和需要用户决策的步骤：
		   - 自动执行步骤标记示例：
		     [START] 正在提取关键词 [DONE]
		     [START] 正在检索视频 [DONE]
		     [START] 检索结果：找到3个相关视频 [DONE]
		     [START] 正在生成报告 [DONE]
		   - 需要用户决策的标记示例：
		     [START] 请选择视频分析方式：
		     1. CV模型：精确度高但处理时间较长
		     2. 大模型：处理速度较快但精确度可能略低 [DONE]
				
		工作流程执行规则（非常重要）：
		
		1. 自动执行步骤必须连续执行，不要等待用户输入：
		   - 关键词提取 → 视频检索 → 展示检索结果，这些步骤应该自动连续执行
		   - 报告生成也是自动执行的步骤
		
		2. 只有在需要用户决策的环节才暂停等待用户输入：
		   - 视频分析方式的选择必须由用户决定
		   - 在用户选择后，继续自动执行后续步骤
		
		3. 完整工作流示例：
		   - 自动执行：提取关键词 → 视频检索 → 展示检索结果
		   - 暂停等待用户决策：请求用户选择分析方式
		   - 用户决策后自动执行：执行分析 → 展示分析结果 → 生成报告
				
		视频分析决策规则：
				
		1. 当用户请求分析视频内容时，必须检查用户是否已明确指定分析方式：
		   - 如用户已明确要求使用"CV模型"或"专业CV模型"，直接调用analyzeVideos
		   - 如用户已明确要求使用"大模型"或"大模型分析"，直接调用analyzeVideoBySlicing
		   - 如用户未明确指定分析方式，必须暂停并请求用户决策
				
		2. 不允许在没有用户明确决策的情况下自行选择分析方式
				
		3. 在用户做出决策后，应立即执行相应的分析工具，并在分析完成后继续执行后续步骤（如生成报告）
		
		4. 对于包含多个请求的复杂查询，应按照逻辑顺序处理，只在需要用户决策的环节暂停
		
		5. 对于"查找视频并分析"这类复合请求，应先完成视频检索，然后再请求用户选择分析方式
				
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
		return chatResponse.getResult()
				.getOutput()
				.getText();
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

	@Resource
	private RestTemplate restTemplate;

	/**
	 * 调用 Bedrock Claude API
	 */
	public void callBedrockApi() {
		String url = "https://bedrock-runtime.us-east-1.amazonaws.com/model/us.anthropic.claude-3-5-haiku-20241022-v1:0/converse";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer ABSKQmVkcm9ja0FQSUtleS1hMGl2LWF0LTcyOTUwNTAwMzU4MTpITTF0ckJ5SE9ZRXI4ZFN0dTNzUHFDamNFb1h3ZDhybE1qREtPbGlJOGJpTlJSeDhwdXlHK29USG1KQT0=");


		HttpEntity<Object> entity = new HttpEntity<>("""
				{
				    "messages": [
				        {
				            "role": "user",
				            "content": [
				                {
				                    "text": "hello"
				                }
				            ]
				        }
				    ]
				}""", headers);

		try {
			ResponseEntity<Object> response = restTemplate.exchange(
					url,
					HttpMethod.POST,
					entity,
					Object.class
			);

			log.info("Bedrock API 调用成功");
			Object body = response.getBody();
			log.info("响应结果: {}", body);
		} catch (Exception e) {
			log.error("Bedrock API 调用失败: {}", e.getMessage(), e);
			throw new RuntimeException("Bedrock API 调用失败", e);
		}
	}
}