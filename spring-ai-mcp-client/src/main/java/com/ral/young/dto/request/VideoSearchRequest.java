package com.ral.young.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.Serializable;
import java.util.List;

/**
 * @author renyh
 * @description 视频搜索请求
 * @date 2025/7/23 10:38
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSearchRequest implements Serializable {

	/**
	 * 关键词列表
	 */
	@ToolParam(description = "从用户查询中提取的核心关键词列表。")
	private List<String> keywords;

	/**
	 * 用户原始查询
	 */
	@ToolParam(description = "用户的原始查询全文，用于支持向量检索和语义理解。")
	private String query;

	/**
	 * 返回结果数量上限
	 */
	@ToolParam(description = "返回结果数量的上限。默认为10。")
	private Integer limit;

	/**
	 * 是否使用向量检索
	 */
	@ToolParam(description = "是否启用向量检索以提高语义匹配的准确性。默认为true。")
	private Boolean useEmbedding;
}
