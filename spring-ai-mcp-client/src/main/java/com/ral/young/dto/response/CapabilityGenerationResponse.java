package com.ral.young.dto.response;

import com.ral.young.enums.TaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author renyh
 * @description 能力生成响应
 * @date 2025/7/25 10:48
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityGenerationResponse implements Serializable {

	/**
	 * 任务ID
	 */
	private Long taskId;

	/**
	 * 能力ID
	 */
	private String capabilityId;

	/**
	 * 能力名称
	 */
	private String capabilityName;

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