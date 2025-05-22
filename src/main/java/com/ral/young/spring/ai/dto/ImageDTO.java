package com.ral.young.spring.ai.dto;

import lombok.Data;

/**
 * @author renyh
 * @description 图片信息
 * @date 2025/5/22 15:13
 * @since 1.0.0
 */
@Data
public class ImageDTO {

	private String imageUrl;

	private String prompt;
}
