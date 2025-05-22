package com.ral.young.spring.ai.model;

import cn.hutool.json.JSONObject;
import com.google.common.collect.Lists;
import com.ral.young.spring.ai.config.CustomOpenAiApi;
import com.ral.young.spring.ai.entity.CustomEmbedding;
import com.ral.young.spring.ai.entity.CustomEmbeddingRequest;
import com.ral.young.spring.ai.entity.CustomEmbeddingResponse;
import lombok.NonNull;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.DocumentEmbeddingModel;
import org.springframework.ai.embedding.DocumentEmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @author renyh
 * @description 自定义多模板的文档嵌入模型
 * @date 2025/5/22 17:06
 * @since 1.0.0
 */
public class CustomDocumentEmbeddingModel implements DocumentEmbeddingModel {

	private final CustomOpenAiApi openAiApi;

	public CustomDocumentEmbeddingModel(CustomOpenAiApi openAiApi) {
		this.openAiApi = openAiApi;
	}

	@Override
	@NonNull
	public EmbeddingResponse call(@NonNull DocumentEmbeddingRequest request) {
		return new EmbeddingResponse(Lists.newArrayList());
	}

	public CustomEmbeddingResponse customCall(@NonNull CustomEmbeddingRequest request) {
		OpenAiApi.EmbeddingRequest<List<JSONObject>> embeddingRequest = new OpenAiApi.EmbeddingRequest<>(
				request.getInputs()
				, request.getOptions().getModel()
		);
		ResponseEntity<OpenAiApi.EmbeddingList<CustomEmbedding>> entity = openAiApi.customEmbeddings(embeddingRequest);
		OpenAiApi.EmbeddingList<CustomEmbedding> embeddingResult = entity.getBody();
		assert embeddingResult != null;

		OpenAiApi.Usage usage = embeddingResult.usage();
		Usage embeddingResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
		var metadata = new EmbeddingResponseMetadata(embeddingResult.model(), embeddingResponseUsage);
		return new CustomEmbeddingResponse(embeddingResult.data(), metadata);
	}

	private DefaultUsage getDefaultUsage(OpenAiApi.Usage usage) {
		return new DefaultUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage);
	}

	@Override
	public int dimensions() {
		return 0;
	}
}
