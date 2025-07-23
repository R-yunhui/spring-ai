package com.ral.young.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * @author renyh
 * @description 轮次数据
 * @date 2025/7/26 10:18
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoundData {

	/**
	 * 轮次id
	 */
	private String roundId;

	/**
	 * 轮次消息
	 */
	private List<Message> messages;

	/**
	 * 轮次下的追问轮次id
	 */
	private List<String> followupRoundIds;

	public RoundData(String roundId, List<Message> messages) {
		this.roundId = roundId;
		this.messages = messages;
	}

	public void addFollowupRoundId(String followupRoundId) {
		if (!this.followupRoundIds.contains(followupRoundId)) {
			this.followupRoundIds.add(followupRoundId);
		}
	}

	public void removeFollowupRoundId(String followupRoundId) {
		this.followupRoundIds.remove(followupRoundId);
	}
}
