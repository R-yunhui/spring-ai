package com.ral.young.memeory;

import cn.hutool.core.util.StrUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * @author renyh
 * @description 支持轮次对话的聊天记忆接口
 * @date 2025/7/26 10:17
 * @since 1.0.0
 */
public interface RoundBasedChatMemory extends ChatMemory {

	String DEFAULT_ROUND_ID = StrUtil.EMPTY;

	Boolean DEFAULT_FOLLOW_UP_QUESTION = false;

	String DEFAULT_FOLLOW_UP_QUESTION_ID = StrUtil.EMPTY;

	String DEFAULT_FOLLOW_UP_QUESTION_REFERENCE_ID = StrUtil.EMPTY;

	/**
	 * 会话内部的对话轮次 ID
	 */
	String ROUND_ID = "chat_memory_round_id";

	/**
	 * 是否是追问
	 */
	String FOLLOW_UP_QUESTION = "follow_up_question";

	/**
	 * 使用的追问轮次 ID
	 */
	String FOLLOW_UP_QUESTION_ID = "follow_up_question_id";

	/**
	 * 追问轮次的原始轮次 ID
	 */
	String FOLLOW_UP_QUESTION_REFERENCE_ID = "follow_up_question_reference_id";

	/**
	 * 创建新的对话轮次
	 *
	 * @param conversationId 会话ID
	 * @return 新创建的轮次ID
	 */
	@NotNull
	String createRound(@NotNull String conversationId);

	/**
	 * 添加消息到指定轮次
	 *
	 * @param conversationId 会话ID
	 * @param roundId        轮次ID
	 * @param messages       消息列表
	 */
	void addToRound(@NotNull String conversationId, @NotNull String roundId, @NotNull List<Message> messages);

	/**
	 * 添加单个消息到指定轮次
	 *
	 * @param conversationId 会话ID
	 * @param roundId        轮次ID
	 * @param message       消息
	 */
	default void addToRound(@NotNull String conversationId, @NotNull String roundId, @NotNull Message message) {
		addToRound(conversationId, roundId, List.of(message));
	}

	/**
	 * 获取指定轮次的消息
	 *
	 * @param conversationId 会话ID
	 * @param roundId        轮次ID
	 * @return 轮次消息列表
	 */
	@NotNull
	List<Message> getRound(@NotNull String conversationId, @NotNull String roundId);

	/**
	 * 创建追问轮次，关联到原始轮次
	 *
	 * @param conversationId   会话ID
	 * @param referenceRoundId 引用的原始轮次ID
	 * @return 新创建的追问轮次ID
	 */
	@NotNull
	String createFollowupRound(@NotNull String conversationId, @NotNull String referenceRoundId);

	/**
	 * 获取追问上下文(原始轮次+追问轮次的消息)
	 *
	 * @param conversationId  会话ID
	 * @param followupRoundId 追问轮次ID
	 * @return 完整上下文消息列表
	 */
	@NotNull
	List<Message> getFollowupContext(@NotNull String conversationId, @NotNull String followupRoundId);

	/**
	 * 清除特定轮次
	 *
	 * @param conversationId 会话ID
	 * @param roundId        轮次ID
	 */
	void clearRound(@NotNull String conversationId, @NotNull String roundId);

	/**
	 * 获取会话中的所有轮次ID
	 *
	 * @param conversationId 会话ID
	 * @return 轮次ID列表
	 */
	@NotNull
	List<String> getRoundIds(@NotNull String conversationId);
}