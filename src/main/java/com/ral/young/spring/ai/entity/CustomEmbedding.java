package com.ral.young.spring.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.ai.embedding.EmbeddingResultMetadata;
import org.springframework.ai.model.ModelResult;
import org.springframework.ai.model.ResultMetadata;

/**
 * @author renyh
 * @description 自定义嵌入模型返回结果
 * @date 2025/5/22 19:14
 * @since 1.0.0
 */
public class CustomEmbedding implements ModelResult<double[]> {

	@Getter
	private final Integer index;

	@JsonIgnore
	private final double[] embedding;

	private final EmbeddingResultMetadata metadata;

	public CustomEmbedding(Integer index, double[] embedding) {
		this.index = index;
		this.embedding = embedding;
		// TODO: 后续自定义嵌入模型返回结果的元数据
		this.metadata = null;
	}

	@Override
	public double[] getOutput() {
		return embedding;
	}

	@Override
	public ResultMetadata getMetadata() {
		return metadata;
	}
}
