package com.ral.young.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author renyh
 * @description 关键词提取响应
 * @date 2025/7/23 10:29
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordExtractResponse implements Serializable {

	/**
	 * 关键词列表
	 */
	private List<String> keywords;

	/**
	 * 用户原始查询
	 */
	private String originalQuery;

	/**
	 * 建议的下一个工具调用
	 */
	private String nextTool;
}