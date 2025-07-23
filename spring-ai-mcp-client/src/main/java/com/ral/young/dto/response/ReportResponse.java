package com.ral.young.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author renyh
 * @description 报告生成响应
 * @date 2025/7/23 10:37
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse implements Serializable {

	/**
	 * 报告ID
	 */
	private String reportId;

	/**
	 * 报告内容
	 */
	private String content;

	/**
	 * 格式(markdown等)
	 */
	private String format;

	/**
	 * 建议的下一个工具调用
	 */
	private String nextTool;

	/**
	 * 创建Markdown格式报告响应
	 */
	public static ReportResponse markdown(
			String reportId,
			String content,
			String nextTool
	) {
		return ReportResponse.builder()
				.reportId(reportId)
				.content(content)
				.format("markdown")
				.nextTool(nextTool)
				.build();
	}
}
