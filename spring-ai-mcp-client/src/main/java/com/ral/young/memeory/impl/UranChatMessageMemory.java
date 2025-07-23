package com.ral.young.memeory.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Lists;
import com.ral.young.dto.FollowupData;
import com.ral.young.dto.RoundData;
import com.ral.young.memeory.RoundBasedChatMemory;
import com.ral.young.memeory.RoundBasedChatMemoryRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author renyh
 * @description 支持轮次对话和追问功能的聊天消息内存
 * @date 2025/7/26 9:45
 * @since 1.0.0
 */
@SuppressWarnings("preview")
public class UranChatMessageMemory implements RoundBasedChatMemory {

	private static final int DEFAULT_MAX_MESSAGES = 20;
	private static final String DEFAULT_ROUND_ID = "default";

	private final RoundBasedChatMemoryRepository repository;
	private final int maxMessages;

	private UranChatMessageMemory(RoundBasedChatMemoryRepository repository, int maxMessages) {
		Assert.notNull(repository, "repository cannot be null");
		Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
		this.repository = repository;
		this.maxMessages = maxMessages;
	}

	@Override
	public void add(@NotNull String conversationId, @NotNull List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");

		// 添加到默认轮次
		addToRound(conversationId, DEFAULT_ROUND_ID, messages);
	}

	@NotNull
	@Override
	public List<Message> get(@NotNull String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");

		// 获取所有普通轮次ID（不包括追问轮次）
		List<String> roundIds = this.repository.findRoundIds(conversationId);

		// 收集所有轮次的消息
		List<Message> allMessages = new ArrayList<>();
		for (String roundId : roundIds) {
			// 确保不是追问轮次
			if (!isFollowupRound(conversationId, roundId)) {
				RoundData roundData = this.repository.findRound(conversationId, roundId);
				if (ObjectUtil.isNotNull(roundData) && CollUtil.isNotEmpty(roundData.getMessages())) {
					allMessages.addAll(roundData.getMessages());
				}
			}
		}

		return allMessages;
	}

	/**
	 * 判断是否为追问轮次
	 *
	 * @param conversationId 会话ID
	 * @param roundId        轮次ID
	 * @return 是否为追问轮次
	 */
	private boolean isFollowupRound(@NotNull String conversationId, @NotNull String roundId) {
		return this.repository.findFollowup(conversationId, roundId) != null;
	}

	@Override
	public void clear(@NotNull String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		this.repository.deleteByConversationId(conversationId);
	}

	@Override
	public @NotNull String createRound(@NotNull String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");

		String roundId = generateRoundId();
		RoundData roundData = new RoundData(roundId, Lists.newArrayList(), Lists.newArrayList());
		this.repository.saveRound(conversationId, roundData);

		return roundId;
	}

	@Override
	public void addToRound(@NotNull String conversationId, @NotNull String roundId, @NotNull List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.hasText(roundId, "roundId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");

		// 获取当前轮次数据
		RoundData roundData = this.repository.findRound(conversationId, roundId);
		if (roundData == null) {
			roundData = new RoundData(roundId, Lists.newArrayList(), Lists.newArrayList());
		}

		// 处理消息
		List<Message> processedMessages = process(roundData.getMessages(), messages);
		roundData.setMessages(processedMessages);

		// 保存轮次数据
		this.repository.saveRound(conversationId, roundData);

		// 如果是默认轮次，同时更新会话级别的消息
		if (DEFAULT_ROUND_ID.equals(roundId)) {
			this.repository.saveAll(conversationId, processedMessages);
		}
	}

	@Override
	public @NotNull List<Message> getRound(@NotNull String conversationId, @NotNull String roundId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.hasText(roundId, "roundId cannot be null or empty");

		RoundData roundData = this.repository.findRound(conversationId, roundId);
		return roundData != null ? roundData.getMessages() : Lists.newArrayList();
	}

	@Override
	public @NotNull String createFollowupRound(@NotNull String conversationId, @NotNull String referenceRoundId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.hasText(referenceRoundId, "referenceRoundId cannot be null or empty");

		// 验证原始轮次是否存在
		RoundData referenceRound = this.repository.findRound(conversationId, referenceRoundId);
		if (referenceRound == null) {
			throw new IllegalArgumentException(STR."Referenced round does not exist: \{referenceRoundId}");
		}

		// 生成新的轮次ID
		String followupRoundId = generateRoundId();

		// 创建追问数据
		FollowupData followupData = new FollowupData(followupRoundId, referenceRoundId, Lists.newArrayList());
		this.repository.saveFollowup(conversationId, followupData);

		return followupRoundId;
	}

	@Override
	public @NotNull List<Message> getFollowupContext(@NotNull String conversationId, @NotNull String followupRoundId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.hasText(followupRoundId, "followupRoundId cannot be null or empty");

		// 获取追问数据
		FollowupData followupData = this.repository.findFollowup(conversationId, followupRoundId);
		if (followupData == null) {
			throw new IllegalArgumentException(STR."Followup round not found: \{followupRoundId}");
		}

		// 获取原始轮次消息
		List<Message> originalMessages = getRound(conversationId, followupData.getReferenceRoundId());

		// 获取追问轮次消息
		List<Message> followupMessages = followupData.getMessages();

		// 合并消息
		List<Message> contextMessages = new ArrayList<>(originalMessages);
		contextMessages.addAll(followupMessages);

		return contextMessages;
	}

	@Override
	public void clearRound(@NotNull String conversationId, @NotNull String roundId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.hasText(roundId, "roundId cannot be null or empty");

		// 检查是否为追问轮次
		FollowupData followupData = this.repository.findFollowup(conversationId, roundId);
		if (followupData != null) {
			// 删除追问数据
			this.repository.deleteFollowup(conversationId, roundId);
		} else {
			// 获取所有引用此轮次的追问
			List<String> followupRoundIds = this.repository.findFollowupRoundIds(conversationId, roundId);

			// 删除所有关联的追问
			for (String followupRoundId : followupRoundIds) {
				this.repository.deleteFollowup(conversationId, followupRoundId);
			}

			// 删除轮次数据
			this.repository.deleteRound(conversationId, roundId);
		}
	}

	@Override
	public @NotNull List<String> getRoundIds(@NotNull String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		return this.repository.findRoundIds(conversationId);
	}

	private String generateRoundId() {
		return STR."round-\{UUID.randomUUID().toString().substring(0, 8)}";
	}

	private List<Message> process(List<Message> memoryMessages, List<Message> newMessages) {
		List<Message> processedMessages = new ArrayList<>();

		Set<Message> memoryMessagesSet = new HashSet<>(memoryMessages);
		boolean hasNewSystemMessage = newMessages.stream()
				.filter(SystemMessage.class::isInstance)
				.anyMatch(message -> !memoryMessagesSet.contains(message));

		memoryMessages.stream()
				.filter(message -> !(hasNewSystemMessage && message instanceof SystemMessage))
				.forEach(processedMessages::add);

		processedMessages.addAll(newMessages);

		if (processedMessages.size() <= this.maxMessages) {
			return processedMessages;
		}

		int messagesToRemove = processedMessages.size() - this.maxMessages;

		List<Message> trimmedMessages = new ArrayList<>();
		int removed = 0;
		for (Message message : processedMessages) {
			if (message instanceof SystemMessage || removed >= messagesToRemove) {
				trimmedMessages.add(message);
			} else {
				removed++;
			}
		}

		return trimmedMessages;
	}

	public static UranChatMessageMemory.Builder builder() {
		return new UranChatMessageMemory.Builder();
	}

	public static final class Builder {

		private RoundBasedChatMemoryRepository repository;

		private int maxMessages = DEFAULT_MAX_MESSAGES;

		private Builder() {
		}

		public UranChatMessageMemory.Builder chatMemoryRepository(RoundBasedChatMemoryRepository repository) {
			this.repository = repository;
			return this;
		}

		public UranChatMessageMemory.Builder maxMessages(int maxMessages) {
			this.maxMessages = maxMessages;
			return this;
		}

		public UranChatMessageMemory build() {
			if (this.repository == null) {
				this.repository = new InMemoryRoundBasedChatMemoryRepository();
			}
			return new UranChatMessageMemory(this.repository, this.maxMessages);
		}
	}
}