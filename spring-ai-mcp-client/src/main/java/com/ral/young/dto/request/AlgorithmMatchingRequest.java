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
 * @description 算法匹配请求
 * @date 2025/7/25 10:30
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmMatchingRequest implements Serializable {

	/**
	 * 用户查询
	 */
	@ToolParam(description = "用户的原始查询全文，用于分析用户意图")
	private String query;

	/**
	 * 视频ID列表
	 */
	@ToolParam(description = "需要分析的视频ID列表")
	private List<String> videoIds;
}