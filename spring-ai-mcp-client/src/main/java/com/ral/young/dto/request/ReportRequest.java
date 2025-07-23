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
 * @description 报告生成请求
 * @date 2025/7/23 10:36
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest implements Serializable {

	/**
	 * 视频ID列表(检索报告时使用)
	 */
	@ToolParam(description = "【可选】视频ID列表，用于生成检索报告。	")
	private List<String> videoIdList;

	/**
	 * 任务ID(可选)
	 */
	@ToolParam(description = "【可选】异步任务ID，用于获取CV模型分析结果并生成报告。")
	private Long taskId;

	/**
	 * 结果ID(可选)
	 */
	@ToolParam(description = "【可选】同步结果ID，用于获取LLM模型分析结果并生成报告。")
	private Long resultId;

	/**
	 * 报告类型
	 */
	@ToolParam(description = "报告类型，可选值：'search'(检索报告)、'analysis'(分析报告)、'comprehensive'(通用报告)")
	private String reportType;

	/**
	 * 用户原始查询(可选)
	 */
	@ToolParam(description = "用户的原始查询，为报告提供核心主题和上下文。")
	private String query;
}
