package com.ral.young.memeory.impl;

import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.ral.young.dto.FollowupData;
import com.ral.young.dto.RoundData;
import com.ral.young.memeory.RoundBasedChatMemoryRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author renyh
 * @description 内存实现的轮次对话存储库
 * @date 2025/7/26 10:20
 * @since 1.0.0
 */
public class InMemoryRoundBasedChatMemoryRepository implements RoundBasedChatMemoryRepository {

	// 会话-轮次结构
	private final Map<String, Map<String, RoundData>> conversationRounds = new ConcurrentHashMap<>();

	// 追问轮次结构
	private final Map<String, Map<String, FollowupData>> followupRounds = new ConcurrentHashMap<>();

	@Override
	public void saveAll(@NotNull String conversationId, @NotNull List<Message> messages) {
		// 默认保存到"default"轮次
		RoundData roundData = findRound(conversationId, "default");
		if (roundData == null) {
			roundData = new RoundData("default", messages);
		} else {
			roundData.setMessages(messages);
		}
		saveRound(conversationId, roundData);
	}

	@NotNull
	@Override
	public List<String> findConversationIds() {
		return new ArrayList<>(this.conversationRounds.keySet());
	}

	@Override
	public @NotNull List<Message> findByConversationId(@NotNull String conversationId) {
		// 默认获取"default"轮次的消息
		RoundData roundData = findRound(conversationId, "default");
		return roundData != null ? roundData.getMessages() : Lists.newArrayList();
	}

	@Override
	public void deleteByConversationId(@NotNull String conversationId) {
		conversationRounds.remove(conversationId);
		followupRounds.remove(conversationId);
	}

	@Override
	public void saveRound(@NotNull String conversationId, @NotNull RoundData roundData) {
		conversationRounds.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
				.put(roundData.getRoundId(), roundData);
	}

	@Override
	public RoundData findRound(@NotNull String conversationId, @NotNull String roundId) {
		return conversationRounds.getOrDefault(conversationId, Map.of())
				.get(roundId);
	}

	@Override
	public void deleteRound(@NotNull String conversationId, @NotNull String roundId) {
		Map<String, RoundData> rounds = conversationRounds.get(conversationId);
		if (rounds != null) {
			rounds.remove(roundId);
		}
	}

	@Override
	public void saveFollowup(@NotNull String conversationId, @NotNull FollowupData followupData) {
		followupRounds.computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
				.put(followupData.getFollowupRoundId(), followupData);

		// 更新原始轮次的追问列表
		RoundData referenceRound = findRound(conversationId, followupData.getReferenceRoundId());
		if (referenceRound != null) {
			referenceRound.addFollowupRoundId(followupData.getFollowupRoundId());
			saveRound(conversationId, referenceRound);
		}
	}

	@Override
	public FollowupData findFollowup(@NotNull String conversationId, @NotNull String followupRoundId) {
		return followupRounds.getOrDefault(conversationId, Map.of())
				.get(followupRoundId);
	}

	@Override
	public void deleteFollowup(@NotNull String conversationId, @NotNull String followupRoundId) {
		Map<String, FollowupData> followups = followupRounds.get(conversationId);
		if (followups != null) {
			FollowupData followupData = followups.remove(followupRoundId);
			if (followupData != null) {
				// 从原始轮次的追问列表中移除
				RoundData referenceRound = findRound(conversationId, followupData.getReferenceRoundId());
				if (referenceRound != null) {
					referenceRound.removeFollowupRoundId(followupRoundId);
					saveRound(conversationId, referenceRound);
				}
			}
		}
	}

	@Override
	public @NotNull List<String> findRoundIds(@NotNull String conversationId) {
		return new ArrayList<>(conversationRounds.getOrDefault(conversationId, Map.of()).keySet());
	}

	@Override
	public @NotNull List<String> findFollowupRoundIds(@NotNull String conversationId, @NotNull String referenceRoundId) {
		RoundData roundData = findRound(conversationId, referenceRoundId);
		return roundData != null ? roundData.getFollowupRoundIds() : Lists.newArrayList();
	}
}
