package com.ral.young.spring.ai.dto;

import com.ral.young.spring.ai.constant.CommonConstant;
import lombok.Data;

/**
 * @author renyh
 * @description 嵌入模型参数
 * @date 2025/5/22 17:58
 * @since 1.0.0
 */
@Data
public class EmbeddingDTO {

	private CommonConstant.EmbeddingType embeddingType;

	private String input;
}
