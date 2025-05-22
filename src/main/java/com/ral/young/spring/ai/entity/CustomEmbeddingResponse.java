package com.ral.young.spring.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.model.ModelResponse;
import org.springframework.ai.model.ResponseMetadata;

import java.util.List;

/**
 * @author renyh
 * @description 定义返回结果
 * @date 2025/5/22 17:49
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomEmbeddingResponse implements ModelResponse<CustomEmbedding> {

	private List<CustomEmbedding> embeddings;

	private EmbeddingResponseMetadata metadata;

	@Override
	public CustomEmbedding getResult() {
		return null;
	}

	@Override
	public List<CustomEmbedding> getResults() {
		return List.of();
	}

	@Override
	public ResponseMetadata getMetadata() {
		return metadata;
	}
}
