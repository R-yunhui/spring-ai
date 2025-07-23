package com.ral.young.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author renyh
 * @description 聊天请求参数
 * @date 2025/7/1 10:37
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRequestVO {

	/**
	 * 用户详细问题
	 */
	private String prompt;

	/**
	 * 异步任务ID
	 */
	private Long taskId;

	/**
	 * 视频ID列表
	 */
	private List<String> videoIds;

	/**
	 * 会话id
	 */
	private String conversationId;

	/**
	 * 追问轮次引用的对话轮次id
	 */
	private String followUpQuestionReferenceId;

	/**
	 * 追问轮次id
	 */
	private String followUpQuestionId;

	/**
	 * 是否为追问
	 */
	private Boolean followUpQuestion;
}
