package com.ral.young.enums;

/**
 * @author renyh
 * @description 任务状态枚举
 * @date 2025/7/23 10:34
 * @since 1.0.0
 */
public enum TaskStatusEnum {
	/**
	 * 待处理
	 */
	PENDING,

	/**
	 * 运行中
	 */
	RUNNING,

	/**
	 * 已完成
	 */
	COMPLETED,

	/**
	 * 失败
	 */
	FAILED,

	/**
	 * 未找到
	 */
	NOT_FOUND
}
