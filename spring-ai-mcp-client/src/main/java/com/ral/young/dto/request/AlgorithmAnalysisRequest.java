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
 * @description 算法分析请求
 * @date 2025/7/25 11:00
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmAnalysisRequest implements Serializable {

	/**
	 * 视频ID列表
	 */
	@ToolParam(description = "需要分析的视频ID列表")
	private List<String> videoIds;

	/**
	 * 算法类型
	 */
	@ToolParam(description = "要使用的算法类型，如PERSON_DETECTION、ANIMAL_DETECTION、TRAFFIC_VIOLATION等")
	private String algorithmType;

	/**
	 * 分析参数
	 */
	@ToolParam(description = "算法分析的额外参数，JSON格式")
	private String analysisParams;

	/**
	 * 聊天ID
	 */
	@ToolParam(description = "当前会话的ID，用于后续通知")
	private Long chatId;
}