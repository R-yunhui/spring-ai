package com.ral.young.spring.ai.model;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ral.young.spring.ai.config.CustomOpenAiApi;
import com.ral.young.spring.ai.model.dto.EmbeddingDTO;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Admin
 * @description TODO
 * @date 2025/5/15 14:47
 * @since 1.0.0
 */
public class CustomEmbeddingModel<T> extends AbstractEmbeddingModel {

	private final CustomOpenAiApi openAiApi;

	@Getter
	private final OpenAiEmbeddingOptions defaultOptions;

	public CustomEmbeddingModel(CustomOpenAiApi openAiApi, OpenAiEmbeddingOptions defaultOptions) {
		this.openAiApi = openAiApi;
		this.defaultOptions = defaultOptions;
	}

	public List<JSONObject> call2(@NotNull List<? extends EmbeddingDTO> embeddingDTOList) {
		List<EmbeddingDTO> curEmbeddingDTOList = new ArrayList<>(embeddingDTOList);
		OpenAiApi.EmbeddingRequest<List<EmbeddingDTO>> listEmbeddingRequest = new OpenAiApi.EmbeddingRequest<>(curEmbeddingDTOList, defaultOptions.getModel());
		return doCall(listEmbeddingRequest);
	}

	@NotNull
	private List<JSONObject> doCall(OpenAiApi.EmbeddingRequest<List<EmbeddingDTO>> listEmbeddingRequest) {
		ResponseEntity<OpenAiApi.EmbeddingList<JSONObject>> embeddings = openAiApi.embeddingsTwo(listEmbeddingRequest);
		OpenAiApi.EmbeddingList<JSONObject> apiEmbeddingResponse = embeddings.getBody();
		assert apiEmbeddingResponse != null;
		List<JSONObject> result = apiEmbeddingResponse.data()
				.stream()
				.toList();

		var metadata = new EmbeddingResponseMetadata(apiEmbeddingResponse.model(),
				getDefaultUsage(apiEmbeddingResponse.usage()));

		return result;
	}

	private DefaultUsage getDefaultUsage(CustomOpenAiApi.Usage usage) {
		return new DefaultUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage);
	}


	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		return null;
	}

	@NotNull
	@Override
	public float[] embed(@NotNull Document document) {
		return new float[0];
	}

}
