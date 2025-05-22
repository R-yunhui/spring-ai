package com.ral.young.spring.ai.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Admin
 * @description TODO
 * @date 2025/5/22 19:14
 * @since 1.0.0
 */
@Data
public class CustomEmbedding {

	@JsonProperty("index")
	Integer index;

	@JsonProperty("embedding")
	BigDecimal[] embedding;

	@JsonProperty("object")
	String object;

	public CustomEmbedding() {
	}

	public CustomEmbedding(Integer index, BigDecimal[] embedding, String object) {
		this.index = index;
		this.embedding = embedding;
		this.object = object;
	}
}
