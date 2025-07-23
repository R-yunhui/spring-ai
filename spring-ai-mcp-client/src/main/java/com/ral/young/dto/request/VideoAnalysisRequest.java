package com.ral.young.dto.request;

import com.ral.young.enums.ModelTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.Serializable;
import java.util.List;

/**
 * @author renyh
 * @description 视频分析请求
 * @date 2025/7/23 10:30
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalysisRequest implements Serializable {

	/**
	 * 视频ID列表
	 */
	@ToolParam(description = "需要分析的一个或多个视频的ID列表。")
	private List<String> videoIds;

	/**
	 * 分析类型
	 */
	@ToolParam(description = "指定分析的侧重点。'event_detection'用于检测特定事件，'behavior_analysis'用于分析行为，'general_summary'用于生成一个全面的视频内容摘要。")
	private String analysisType;

	/**
	 * 用户查询
	 */
	@ToolParam(description = "用户的原始查询全文，用于分析用户意图。")
	private String query;
}
