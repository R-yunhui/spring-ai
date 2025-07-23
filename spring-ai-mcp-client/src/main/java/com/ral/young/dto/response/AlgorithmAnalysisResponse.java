package com.ral.young.dto.response;

import com.ral.young.enums.TaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author renyh
 * @description 算法分析响应
 * @date 2025/7/25 11:03
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmAnalysisResponse implements Serializable {

	/**
	 * 任务ID
	 */
	private Long taskId;

	/**
	 * 算法类型
	 */
	private String algorithmType;

	/**
	 * 视频ID列表
	 */
	private List<String> videoIds;

	/**
	 * 任务状态
	 */
	private TaskStatusEnum status;

	/**
	 * 预计完成时间（秒）
	 */
	private Integer estimatedCompletionTime;

	/**
	 * 聊天ID
	 */
	private Long chatId;

	/**
	 * 下一个工具
	 */
	private String nextTool;
}