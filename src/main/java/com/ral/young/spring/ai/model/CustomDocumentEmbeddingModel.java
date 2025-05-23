package com.ral.young.spring.ai.model;

import cn.hutool.json.JSONObject;
import com.ral.young.spring.ai.config.CustomOpenAiApi;
import com.ral.young.spring.ai.entity.CustomEmbedding;
import com.ral.young.spring.ai.entity.CustomEmbeddingRequest;
import com.ral.young.spring.ai.entity.CustomEmbeddingResponse;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.model.Model;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author renyh
 * @description 自定义多模板的文档嵌入模型
 * @date 2025/5/22 17:06
 * @since 1.0.0
 */
public class CustomDocumentEmbeddingModel implements Model<CustomEmbeddingRequest, CustomEmbeddingResponse> {

	private final CustomOpenAiApi openAiApi;

	@Getter
	private final String model;

	public CustomDocumentEmbeddingModel(CustomOpenAiApi openAiApi, String model) {
		this.openAiApi = openAiApi;
		this.model = model;
	}

	public CustomEmbeddingResponse call(@NonNull CustomEmbeddingRequest request) {
		String model = Optional.ofNullable(request.getOptions().getModel()).orElse(this.model);
		OpenAiApi.EmbeddingRequest<List<JSONObject>> embeddingRequest = new OpenAiApi.EmbeddingRequest<>(
				request.getInputs()
				, model
		);
		ResponseEntity<OpenAiApi.EmbeddingList<CustomOpenAiApi.CurEmbedding>> entity = openAiApi.customEmbeddings(embeddingRequest);
		OpenAiApi.EmbeddingList<CustomOpenAiApi.CurEmbedding> embeddingResult = entity.getBody();
		assert embeddingResult != null;

		OpenAiApi.Usage usage = embeddingResult.usage();
		Usage embeddingResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
		var metadata = new EmbeddingResponseMetadata(embeddingResult.model(), embeddingResponseUsage);
		List<CustomEmbedding> embeddings = embeddingResult.data()
				.stream()
				.map(embedding -> new CustomEmbedding(embedding.index(), embedding.embedding()))
				.toList();
		return new CustomEmbeddingResponse(embeddings, metadata);
	}

	private DefaultUsage getDefaultUsage(OpenAiApi.Usage usage) {
		return new DefaultUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage);
	}


}
