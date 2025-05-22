package com.ral.young.spring.ai.service;

import cn.hutool.json.JSONObject;
import com.ral.young.spring.ai.constant.CommonConstant;
import com.ral.young.spring.ai.dto.EmbeddingDTO;
import com.ral.young.spring.ai.dto.ImageDTO;
import com.ral.young.spring.ai.entity.CustomEmbeddingRequest;
import com.ral.young.spring.ai.entity.CustomEmbeddingResponse;
import com.ral.young.spring.ai.model.CustomDocumentEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author renyh
 * @description 测试接口定义
 * @date 2025/5/21 16:50
 * @since 1.0.0
 */
@Service
@Slf4j
public class OpenAiService {

	private final ChatClient chatClient;

	private final CustomDocumentEmbeddingModel embeddingModel;

	private final DeepSeekChatModel deepSeekChatModel;

	public OpenAiService(OpenAiChatModel openAiChatModel, CustomDocumentEmbeddingModel embeddingModel, DeepSeekChatModel deepSeekChatModel) {
		this.chatClient = ChatClient.builder(openAiChatModel).defaultSystem("你是一个助手，回答问题的同时，保持语言的简洁和专业。回复的结果控制在200字左右。").build();
		this.embeddingModel = embeddingModel;
		this.deepSeekChatModel = deepSeekChatModel;
	}

	public String chat(String prompt) {
		OpenAiChatOptions options = OpenAiChatOptions.builder()
				// 指定模型
				.model("qwen2.5-72b-instruct").temperature(0.7).build();
		Prompt userPrompt = new Prompt(prompt, options);
		ChatResponse chatResponse = chatClient.prompt(userPrompt).call().chatResponse();
		assert chatResponse != null;
		return chatResponse.getResult().getOutput().getText();
	}

	public String chatWithImage(ImageDTO imageDTO) {
		try {
			OpenAiChatOptions options = OpenAiChatOptions.builder().model("qwen2.5-72b-instruct").temperature(0.7).build();
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
		OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder().model("multimodal-embedding").build();
		List<JSONObject> documents = embeddingDTOList.stream().map(embeddingDTO ->
				switch (embeddingDTO.getEmbeddingType()) {
					case CommonConstant.EmbeddingType.TEXT_EMBEDDING ->
							new JSONObject("text", embeddingDTO.getInput());
					case CommonConstant.EmbeddingType.IMAGE_EMBEDDING ->
							new JSONObject("image", embeddingDTO.getInput());
					default -> {
						log.error("不支持的embedding类型:{}", embeddingDTO.getEmbeddingType());
						yield null;
					}
				}
		).filter(Objects::nonNull).toList();
		CustomEmbeddingRequest embeddingRequest = new CustomEmbeddingRequest(documents, options);
		return embeddingModel.call(embeddingRequest);
	}
}
