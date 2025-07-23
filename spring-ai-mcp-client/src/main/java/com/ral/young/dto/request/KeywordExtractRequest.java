package com.ral.young.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.Serializable;

/**
 * @author renyh
 * @description 关键词提取请求
 * @date 2025/7/23 10:28
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordExtractRequest implements Serializable {

	/**
	 * 用户查询
	 */
	@ToolParam(description = "用户的原始查询全文，例如'查找穿着黑色上衣的男人'")
	private String query;
}
