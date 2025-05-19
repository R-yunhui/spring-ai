package com.ral.young.spring.ai.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.util.Lists;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.model.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author renyh
 * @description 阿里百炼大模型
 * @date 2025/4/23 15:12
 * @since 1.0.0
 */
@Service
@Slf4j
public class DashScopeService {

	private final ChatClient textChatClient;

	private final ChatClient imageChatClient;

	private final DashScopeEmbeddingModel dashScopeEmbeddingModel;

	private final DashScopeImageModel imageModel;

	public DashScopeService(DashScopeChatModel dashScopeChatModel, DashScopeEmbeddingModel dashScopeEmbeddingModel, DashScopeImageModel imageModel) {
		this.textChatClient = ChatClient.builder(dashScopeChatModel)
				.defaultSystem("你是一个善于回答问题的聊天助手。")
				.defaultOptions(DashScopeChatOptions.builder()
						.withModel("qwen-max")
						.withMaxToken(200)
						.withTemperature(0.3)
						.build())
				.build();
		this.imageChatClient = ChatClient.builder(dashScopeChatModel)
				.defaultSystem("你是一个善于进行图片理解的助手。")
				.defaultOptions(DashScopeChatOptions.builder()
						.withModel("qwen-vl-max-latest")
						.withMultiModel(true)
						.build())
				.build();
		this.dashScopeEmbeddingModel = dashScopeEmbeddingModel;
		this.imageModel = imageModel;
	}

	public String textChat(String prompt) {
		return textChatClient.prompt(prompt + "，大概用150字回答即可").call().content();
	}

	public String textImageChat(String prompt, String fileUrl) {
		try {
			UserMessage userMessage = new UserMessage(prompt, Media.builder()
					.data(new URI(fileUrl).toURL())
					.mimeType(MimeTypeUtils.IMAGE_PNG)
					.build());
			userMessage.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);
			Prompt userPrompt = new Prompt(userMessage);
			return imageChatClient.prompt(userPrompt).call().content();
		} catch (Exception e) {
			log.error("调用大模型接口分析图片失败", e);
			return "调用大模型接口分析图片失败";
		}
	}

	public Flux<String> textStreamChat(String prompt) {
		// 转换为 SSE 格式，添加 data: 前缀和换行符
		return textChatClient.prompt(prompt + "，大概用150字回答即可")
				.stream()
				.content()
				.map(content -> "data: " + content + "\n");
	}

	public Set<String> imageChat(String prompt) {
		ImageOptions options = ImageOptionsBuilder.builder()
				.model("wanx-v1")
				.N(1)
				.build();
		ImageResponse response = imageModel.call(new ImagePrompt(prompt, options));
		return response.getResults().stream().map(result -> result.getOutput().getUrl()).collect(Collectors.toSet());
	}

	public Embedding textEmbedding(String prompt) {
		DashScopeEmbeddingOptions dashScopeEmbeddingOptions = new DashScopeEmbeddingOptions();
		dashScopeEmbeddingOptions.setModel("multimodal-embedding-v1");
		EmbeddingResponse embeddingResponse = dashScopeEmbeddingModel.call(new EmbeddingRequest(Lists.newArrayList(prompt), dashScopeEmbeddingOptions));
		return embeddingResponse.getResult();
	}
}
