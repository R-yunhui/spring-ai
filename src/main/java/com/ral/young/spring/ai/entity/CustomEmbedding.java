package com.ral.young.spring.ai.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.ai.model.ModelResult;
import org.springframework.ai.model.ResultMetadata;

import java.math.BigDecimal;

/**
 * @author Admin
 * @description TODO
 * @date 2025/5/22 19:14
 * @since 1.0.0
 */
@Data
public class CustomEmbedding implements ModelResult<BigDecimal[]> {

	@JsonProperty("index")
	private Integer index;

	@JsonProperty("embedding")
	private BigDecimal[] embedding;

	@JsonProperty("object")
	private String object;

	public CustomEmbedding() {
	}

	public CustomEmbedding(Integer index, BigDecimal[] embedding, String object) {
		this.index = index;
		this.embedding = embedding;
		this.object = object;
	}

	@Override
	public BigDecimal[] getOutput() {
		return embedding;
	}

	@Override
	public ResultMetadata getMetadata() {
		return null;
	}
}
