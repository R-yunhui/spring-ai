package com.ral.young.spring.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.ral.young.spring.ai.constant.CommonConstant;
import com.ral.young.spring.ai.dto.EmbeddingDTO;
import com.ral.young.spring.ai.dto.ImageDTO;
import com.ral.young.spring.ai.entity.CustomEmbeddingRequest;
import com.ral.young.spring.ai.entity.CustomEmbeddingResponse;
import com.ral.young.spring.ai.memory.CustomChatMemoryRepository;
import com.ral.young.spring.ai.model.CustomDocumentEmbeddingModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.MysqlChatMemoryRepositoryDialect;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

/**
 * @author renyh
 * @description 测试接口定义
 * @date 2025/5/21 16:50
 * @since 1.0.0
 */
@Slf4j
@Service
@SuppressWarnings("preview")
public class OpenAiService {

	private final ChatClient chatClient;

	private final ChatClient inDbMemoryChatClient;

	private final CustomDocumentEmbeddingModel embeddingModel;

	private final DeepSeekChatModel deepSeekChatModel;

	@Resource
	private ToolService toolService;

	public OpenAiService(OpenAiChatModel openAiChatModel, @Qualifier(value = "multimodalEmbedding") CustomDocumentEmbeddingModel embeddingModel, DeepSeekChatModel deepSeekChatModel, JdbcTemplate jdbcTemplate, RedisTemplate<String, Object> redisTemplate) {
		MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor
				.builder(MessageWindowChatMemory
						.builder()
						.maxMessages(10)
						.chatMemoryRepository(new InMemoryChatMemoryRepository())
						.build())
				.build();

		this.chatClient = ChatClient
				.builder(openAiChatModel)
				.defaultSystem("你是一个助手，回答问题的同时，保持语言的简洁和专业。回复的结果控制在200字左右。")
				.build();
		this.embeddingModel = embeddingModel;
		this.deepSeekChatModel = deepSeekChatModel;

		PromptChatMemoryAdvisor dbChatMemoryAdvisor = PromptChatMemoryAdvisor
				.builder(MessageWindowChatMemory
						.builder()
						.maxMessages(10)
						.chatMemoryRepository(CustomChatMemoryRepository
								.builder()
								.jdbcTemplate(jdbcTemplate)
								.redisTemplate(redisTemplate)
								.dialect(new MysqlChatMemoryRepositoryDialect())
								.build())
						.build())
				.build();
		this.inDbMemoryChatClient = ChatClient
				.builder(openAiChatModel)
				.defaultSystem("""
							你是一个智能助手，需严格遵循以下交互规则：
							1. 请求分类处理：
							   - 普通对话（问候/咨询）：直接友好回答
							   - 工具型请求：按步骤执行并实时反馈
							2. 工具调用规范：
							   ■ 每次函数调用后立即暂停
							   ■ 反馈三要素：
							      ✓ 执行操作（函数名）
							      ✓ 关键结果数据
							      ✓ 后续行动计划
							   ■ 多步骤示例：
							      [获取时间] → [验证地区] → [执行任务]
							3. 错误处理：
							   - 即时说明失败原因
							   - 提供修正建议
							4. 地区规则：
							   - 延迟任务必须校验北京地区
							   - 用户显式声明地区时可跳过校验
						""")
				.defaultAdvisors(List.of(dbChatMemoryAdvisor))
				.build();
	}

	public String chat(String prompt) {
		OpenAiChatOptions options = OpenAiChatOptions.builder()
				// 指定模型
				.model("qwen-vl-72b").temperature(0.7).build();
		Prompt userPrompt = new Prompt(prompt, options);
		ChatResponse chatResponse = chatClient.prompt(userPrompt).call().chatResponse();
		assert chatResponse != null;
		return chatResponse.getResult().getOutput().getText();
	}

	public Flux<String> chatWithConversationId(String prompt, String id) {
		OpenAiChatOptions options = OpenAiChatOptions.builder().model("qwen-vl-72b").temperature(0.7).build();
		Prompt userPrompt = new Prompt(prompt, options);
		return inDbMemoryChatClient.prompt(userPrompt).advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, id)).stream().content();
	}

	public String chatWithImage(ImageDTO imageDTO) {
		try {
			OpenAiChatOptions options = OpenAiChatOptions.builder().model("uranmm-40B").temperature(0.7).build();
			Media media = Media.builder().data(imageDTO.getImageUrl()).mimeType(MimeTypeUtils.IMAGE_JPEG).build();
			UserMessage userMessage = UserMessage.builder().text(imageDTO.getPrompt()).media(media).build();
			Prompt userPrompt = new Prompt(Collections.singletonList(userMessage), options);
			ChatResponse chatResponse = chatClient.prompt(userPrompt).call().chatResponse();
			assert chatResponse != null;
			return chatResponse.getResult().getOutput().getText();
		} catch (Exception e) {
			log.error("调用openai接口异常", e);
			throw new RuntimeException(e);
		}
	}

	public Flux<String> chatStream(String prompt) {
		OpenAiChatOptions options = OpenAiChatOptions.builder().model("qwen2.5-72b-instruct").temperature(0.7).streamUsage(true) // 开启流式传输
				.build();
		Prompt userPrompt = new Prompt(prompt, options);
		return chatClient.prompt(userPrompt).stream().content();
	}

	public String deepSeek(String prompt) {
		OpenAiChatOptions options = OpenAiChatOptions.builder().model("deepseek-r1").temperature(0.7).build();
		Prompt userPrompt = new Prompt(prompt, options);
		ChatResponse chatResponse = deepSeekChatModel.call(userPrompt);
		assert chatResponse != null;
		DeepSeekAssistantMessage output = (DeepSeekAssistantMessage) chatResponse.getResult().getOutput();
		JSONObject object = new JSONObject();
		object.putOpt("content", output.getText());
		object.putOpt("reasoning-content", output.getReasoningContent());
		return object.toString();
	}

	public CustomEmbeddingResponse embedding(List<EmbeddingDTO> embeddingDTOList) {
		OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder().model(embeddingModel.getModel()).build();
		List<JSONObject> documents = embeddingDTOList.stream().map(embeddingDTO -> switch (embeddingDTO.getEmbeddingType()) {
			case CommonConstant.EmbeddingType.TEXT_EMBEDDING -> {
				JSONObject object = new JSONObject();
				object.putOpt("text", embeddingDTO.getInput());
				yield object;
			}
			case CommonConstant.EmbeddingType.IMAGE_EMBEDDING -> {
				JSONObject object = new JSONObject();
				object.putOpt("image", embeddingDTO.getInput());
				yield object;
			}
			default -> {
				log.error("不支持的embedding类型:{}", embeddingDTO.getEmbeddingType());
				yield null;
			}
		}).toList();
		CustomEmbeddingRequest embeddingRequest = new CustomEmbeddingRequest(documents, options);
		return embeddingModel.call(embeddingRequest);
	}

	public Flux<String> testTool(ImageDTO imageDTO) {
		OpenAiChatOptions options = OpenAiChatOptions
				.builder()
				.model("qwen2.5-72b-instruct")
				.temperature(0.7)
				.build();
		UserMessage.Builder builder = UserMessage.builder()
				.text(STR."""
						用户输入：
						\{imageDTO.getPrompt()}

						请按以下要求进行响应：
						1. 如果是问候/常识问题，直接回答
						2. 涉及工具调用时：
						   - 分步执行并实时报告
						   - 保持地区校验逻辑
						3. 回答需：
						   - 普通对话：简洁友好
						   - 工具操作：结构化反馈
						""");
		if (StrUtil.isNotBlank(imageDTO.getImageUrl())) {
			Media media = Media.builder()
					.data(imageDTO.getImageUrl())
					.mimeType(MimeTypeUtils.IMAGE_JPEG)
					.build();
			builder.media(media);
		}
		UserMessage userMessage = builder.build();
		return inDbMemoryChatClient.prompt(new Prompt(Collections.singletonList(userMessage), options))
				.tools(toolService)
				.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, imageDTO.getId()))
				.stream()
				.content();
	}
}
