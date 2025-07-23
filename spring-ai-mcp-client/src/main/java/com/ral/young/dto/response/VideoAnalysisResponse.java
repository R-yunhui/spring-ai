package com.ral.young.dto.response;

import com.ral.young.enums.TaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author renyh
 * @description 视频分析响应
 * @date 2025/7/23 10:30
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalysisResponse implements Serializable {

	/**
	 * 任务ID (异步分析时)
	 */
	private Long taskId;

	/**
	 * 任务状态
	 */
	private TaskStatusEnum status;

	/**
	 * 如果是同步分析的时候，比如CV模型分析和能力生成都不可用，走大模型分析就走这个，后续从缓存里面获取到分析的结果
	 */
	private Long resultId;
}
