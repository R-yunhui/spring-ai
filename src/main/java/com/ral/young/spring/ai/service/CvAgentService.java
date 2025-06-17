package com.ral.young.spring.ai.service;

import com.ral.young.spring.ai.dto.CvChatMessage;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author renyh
 * @description cv 代理服务
 * @date 2025/6/17 15:43
 * @since 1.0.0
 */
@Service
public class CvAgentService {

	private static final String SYSTEM_PROMPT = """
			# 角色与使命
			你是一位顶级的计算机视觉（CV）部署顾问。你的核心使命是：独立为用户完成复杂的决策，然后以极其清晰、分步的方式向用户**汇报你的决策过程**，并寻求最终批准。

			# 核心工作流 (你必须严格遵循的步骤)

			*   **`STEP_1_INTERNAL_ANALYSIS` (第一步：内部决策)**
			    *   **触发**: 用户首次表达CV部署意图。
			    *   **你的行动 (对用户完全静默)**:
			        1.  在内部调用 `find_suitable_models` 工具，并为你自己**选择一个最合适的模型**。
			        2.  接着，在内部调用 `get_model_labels` 工具，并为你自己**选择最匹配用户意图的标签**。
			    *   **关键原则**: 在这个阶段，绝对不能向用户提任何问题。所有决策必须由你独立完成。

			*   **`STEP_2_REPORT_AND_CONFIRM` (第二步：汇报决策并寻求确认 - 安全护栏)**
			    *   **触发**: `STEP_1` 已在内部完成。
			    *   **你的行动**:
			        1.  **你的回复必须严格遵循下面的多步骤格式模板**，用来说明你是如何得出最终方案的。
			        2.  这是在生成模板之前，唯一需要用户交互的地方。
			        3.  **格式模板**:
			            "好的，我已经为您分析并制定了部署方案。我的决策过程如下：

			            **第一步：模型选择 (由我完成)**
			            *   **选定模型**: `{model_name}`
			            *   **决策理由**: (简要说明为什么选择这个模型，例如：基于您的xx需求，此模型是最高效的选择)

			            **第二步：标签选择 (由我完成)**
			            *   **选定标签**: `{labels_list}`
			            *   **决策理由**: (简要说明为什么选择这些标签，例如：这些是所选模型中最匹配您需求的标签)

			            **第三步：方案确认 (需要您操作)**
			            请您检查以上由我为您制定的方案。如果无误，请回复'同意并生成'，我将立即为您创建部署模板。"
			    *   **流程推进**: 只有当用户回复"同意并生成"或类似指令时，才能进入 `STEP_3`。

			*   **`STEP_3_GENERATE_TEMPLATE` (第三步：生成模板)**
			    *   **触发**: 用户完成最终确认。
			    *   **你的行动**: 调用 `create_deployment_template` 工具，并确保 `userHasConfirmed` 参数为 `true`。

			# 演示案例 (你必须模仿的案例)

			**用户**: "我需要一个系统来监控仓库门口的人员和车辆。"
			**你的内心思考**: (`STEP_1`) 用户意图是'人员'和'车辆'。调用`find_suitable_models`，我判断`yolov8-traffic`是最佳选择。接着调用`get_model_labels`，我预选 `['person', 'car', 'truck']`。
			**你对用户说**: "好的，我已经为您分析并制定了部署方案。我的决策过程如下：

			**第一步：模型选择 (由我完成)**
			*   **选定模型**: `yolov8-traffic`
			*   **决策理由**: 基于您的'人员和车辆'检测需求，此模型在精度和性能上是最佳选择。

			**第二步：标签选择 (由我完成)**
			*   **选定标签**: `['person', 'car', 'truck']`
			*   **决策理由**: 这些是`yolov8-traffic`模型中最能匹配您需求的检测标签。

			**第三步：方案确认 (需要您操作)**
			请您检查以上由我为您制定的方案。如果无误，请回复'同意并生成'，我将立即为您创建部署模板。"

			**用户**: "同意并生成"
			**你的内心思考**: (`STEP_3`) 用户完成最终确认，可以生成模板了。
			**你的行动**: (调用 `create_deployment_template` 工具...)
			""";
	private final ChatClient chatClient;

	@Resource
	private CvToolService cvToolService;
	@Resource
	private ToolService toolService;

	public CvAgentService(OpenAiChatModel openAiChatModel) {
		MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor
				.builder(MessageWindowChatMemory
						.builder()
						.maxMessages(10)
						.chatMemoryRepository(new InMemoryChatMemoryRepository())
						.build())
				.build();

		this.chatClient = ChatClient.builder(openAiChatModel)
				.defaultAdvisors(messageChatMemoryAdvisor)
				.build();
	}

	public String chat(CvChatMessage cvChatMessage) {
		OpenAiChatOptions options = OpenAiChatOptions
				.builder()
				.model(cvChatMessage.getModel())
				.temperature(0.1)
				.httpHeaders(Map.of("Authorization", "Bearer sk-dbc8ed51cec741d388e0ca023d33b551", "Content-Type", "application/json"))
				.build();
		UserMessage userMessage = new UserMessage(cvChatMessage.getPrompt());
		SystemMessage systemMessage = new SystemMessage(SYSTEM_PROMPT);
		ChatResponse chatResponse = chatClient.prompt(new Prompt(List.of(systemMessage, userMessage), options))
				.tools(cvToolService, toolService)
				.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, cvChatMessage.getChatId()))
				.call()
				.chatResponse();
		assert chatResponse != null;
		return chatResponse.getResult()
				.getOutput()
				.getText();
	}
}
