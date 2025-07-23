package com.ral.young.memeory;

import com.ral.young.dto.FollowupData;
import com.ral.young.dto.RoundData;
import org.springframework.ai.chat.memory.ChatMemoryRepository;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author renyh
 * @description 支持轮次对话的聊天记忆存储库接口
 * @date 2025/7/26 10:17
 * @since 1.0.0
 */
public interface RoundBasedChatMemoryRepository extends ChatMemoryRepository {

	/**
	 * 保存轮次数据
	 * @param conversationId 会话ID
	 * @param roundData 轮次数据
	 */
	void saveRound(@NotNull String conversationId, @NotNull RoundData roundData);

	/**
	 * 获取轮次数据
	 * @param conversationId 会话ID
	 * @param roundId 轮次ID
	 * @return 轮次数据，如果不存在则返回null
	 */
	RoundData findRound(@NotNull String conversationId, @NotNull String roundId);

	/**
	 * 删除轮次数据
	 * @param conversationId 会话ID
	 * @param roundId 轮次ID
	 */
	void deleteRound(@NotNull String conversationId, @NotNull String roundId);

	/**
	 * 保存追问数据
	 * @param conversationId 会话ID
	 * @param followupData 追问数据
	 */
	void saveFollowup(@NotNull String conversationId, @NotNull FollowupData followupData);

	/**
	 * 获取追问数据
	 * @param conversationId 会话ID
	 * @param followupRoundId 追问轮次ID
	 * @return 追问数据，如果不存在则返回null
	 */
	FollowupData findFollowup(@NotNull String conversationId, @NotNull String followupRoundId);

	/**
	 * 删除追问数据
	 * @param conversationId 会话ID
	 * @param followupRoundId 追问轮次ID
	 */
	void deleteFollowup(@NotNull String conversationId, @NotNull String followupRoundId);

	/**
	 * 获取会话中的所有轮次ID
	 * @param conversationId 会话ID
	 * @return 轮次ID列表
	 */
	@NotNull
	List<String> findRoundIds(@NotNull String conversationId);

	/**
	 * 获取引用指定轮次的所有追问轮次ID
	 * @param conversationId 会话ID
	 * @param referenceRoundId 引用的轮次ID
	 * @return 追问轮次ID列表
	 */
	@NotNull
	List<String> findFollowupRoundIds(@NotNull String conversationId, @NotNull String referenceRoundId);
}
