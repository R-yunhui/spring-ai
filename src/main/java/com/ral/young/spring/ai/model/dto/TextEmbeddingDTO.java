package com.ral.young.spring.ai.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author Admin
 * @description TODO
 * @date 2025/5/14 18:33
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TextEmbeddingDTO extends EmbeddingDTO {

	@JsonProperty("text")
	private String text;
}
