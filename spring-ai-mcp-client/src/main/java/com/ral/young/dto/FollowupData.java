package com.ral.young.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * @author renyh
 * @description 追问数据
 * @date 2025/7/26 10:19
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowupData {

	/**
	 * 追问轮次id
	 */
	private String followupRoundId;

	/**
	 * 引用轮次id
	 */
	private String referenceRoundId;

	/**
	 * 追问内容
	 */
	private List<Message> messages;
}
