package com.ral.young.spring.ai.memory;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author renyh
 * @description 自定义上下文管理
 * @date 2025/6/11 17:05
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class CustomChatMemoryRepository implements ChatMemoryRepository {

	private final JdbcTemplate jdbcTemplate;

	private final TransactionTemplate transactionTemplate;

	private final JdbcChatMemoryRepositoryDialect dialect;

	private final RedisTemplate<String, Object> redisTemplate;

	private final String CONVERSATION_CONTENT = "conversation::content";

	public CustomChatMemoryRepository(JdbcTemplate jdbcTemplate, PlatformTransactionManager txManager,
									  JdbcChatMemoryRepositoryDialect dialect, RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		Assert.notNull(dialect, "dialect cannot be null");
		this.jdbcTemplate = jdbcTemplate;
		this.dialect = dialect;
		this.transactionTemplate = new TransactionTemplate(
				txManager != null ? txManager : new DataSourceTransactionManager(Objects.requireNonNull(jdbcTemplate.getDataSource())));
	}

	@NotNull
	@Override
	public List<String> findConversationIds() {
		HashOperations<String, String, List<String>> hashOps = redisTemplate.opsForHash();
		Map<String, List<String>> entries = hashOps.entries(CONVERSATION_CONTENT);
		if (MapUtil.isEmpty(entries)) {
			return this.jdbcTemplate.queryForList(dialect.getSelectConversationIdsSql(), String.class);
		}
		return entries.keySet().stream().toList();
	}

	@NotNull
	@Override
	public List<Message> findByConversationId(@NotNull String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
		String redisMessage = hashOps.get(CONVERSATION_CONTENT, conversationId);
		List<Message> messages;
		if (StrUtil.isBlank(redisMessage)) {
			messages = this.jdbcTemplate.query(this.dialect.getSelectMessagesSql(), new MessageRowMapper(), conversationId);
		} else {
			JSONArray array = JSONUtil.parseArray(redisMessage);
			messages = array.stream().map(message -> {
				var obj = JSONUtil.parseObj(message);
				var type = MessageType.fromValue(obj.getStr("type"));
				var content = obj.getStr("content");
				return getAbstractMessage(type, content);
			}).toList();
		}
		return messages;
	}

	private static @NotNull Message getAbstractMessage(MessageType type, String content) {
		return switch (type) {
			case USER -> new UserMessage(content);
			case ASSISTANT -> new AssistantMessage(content);
			case SYSTEM -> new SystemMessage(content);
			// The content is always stored empty for ToolResponseMessages.
			// If we want to capture the actual content, we need to extend
			// AddBatchPreparedStatement to support it.
			case TOOL -> new ToolResponseMessage(List.of());
		};
	}

	@Override
	public void saveAll(@NotNull String conversationId, @NotNull List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");

		this.transactionTemplate.execute(_ -> {
			deleteByConversationId(conversationId);
			this.jdbcTemplate.batchUpdate(this.dialect.getInsertMessageSql(),
					new AddBatchPreparedStatement(conversationId, messages));
			return null;
		});

		// 使用更简洁的JSON格式存储
		JSONArray messageArray = new JSONArray();
		messages.forEach(message -> {
			JSONObject messageObj = new JSONObject();
			messageObj.set("type", message.getMessageType().getValue());
			messageObj.set("content", message.getText());
			messageArray.add(messageObj);
		});
		
		redisTemplate.opsForHash().put(CONVERSATION_CONTENT, conversationId, messageArray.toString());
	}

	@Override
	public void deleteByConversationId(@NotNull String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		redisTemplate.opsForHash().delete(CONVERSATION_CONTENT, conversationId);
		this.jdbcTemplate.update(this.dialect.getDeleteMessagesSql(), conversationId);
	}

	public static CustomChatMemoryRepository.Builder builder() {
		return new CustomChatMemoryRepository.Builder();
	}

	private record AddBatchPreparedStatement(String conversationId, List<Message> messages,
											 AtomicLong instantSeq) implements BatchPreparedStatementSetter {

		private AddBatchPreparedStatement(String conversationId, List<Message> messages) {
			this(conversationId, messages, new AtomicLong(Instant.now().toEpochMilli()));
		}

		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			var message = this.messages.get(i);
			ps.setString(1, this.conversationId);
			ps.setString(2, message.getText());
			ps.setString(3, message.getMessageType().name());
			ps.setTimestamp(4, new Timestamp(this.instantSeq.getAndIncrement()));
		}

		@Override
		public int getBatchSize() {
			return this.messages.size();
		}
	}

	private static class MessageRowMapper implements RowMapper<Message> {

		@Override
		@Nullable
		public Message mapRow(ResultSet rs, int i) throws SQLException {
			var content = rs.getString(1);
			var type = MessageType.valueOf(rs.getString(2));

			return getAbstractMessage(type, content);
		}

	}

	public static final class Builder {

		private JdbcTemplate jdbcTemplate;

		private JdbcChatMemoryRepositoryDialect dialect;

		private DataSource dataSource;

		private PlatformTransactionManager platformTransactionManager;

		private RedisTemplate<String, Object> redisTemplate;

		private static final Logger logger = LoggerFactory.getLogger(JdbcChatMemoryRepository.Builder.class);

		private Builder() {
		}

		public CustomChatMemoryRepository.Builder jdbcTemplate(JdbcTemplate jdbcTemplate) {
			this.jdbcTemplate = jdbcTemplate;
			return this;
		}

		public CustomChatMemoryRepository.Builder dialect(JdbcChatMemoryRepositoryDialect dialect) {
			this.dialect = dialect;
			return this;
		}

		public CustomChatMemoryRepository.Builder dataSource(DataSource dataSource) {
			this.dataSource = dataSource;
			return this;
		}

		public CustomChatMemoryRepository.Builder transactionManager(PlatformTransactionManager txManager) {
			this.platformTransactionManager = txManager;
			return this;
		}

		public CustomChatMemoryRepository.Builder redisTemplate(RedisTemplate<String, Object> redisTemplate) {
			this.redisTemplate = redisTemplate;
			return this;
		}

		public CustomChatMemoryRepository build() {
			DataSource effectiveDataSource = resolveDataSource();
			JdbcChatMemoryRepositoryDialect effectiveDialect = resolveDialect(effectiveDataSource);
			return new CustomChatMemoryRepository(resolveJdbcTemplate(),
					this.platformTransactionManager, effectiveDialect, this.redisTemplate);
		}

		private JdbcTemplate resolveJdbcTemplate() {
			if (this.jdbcTemplate != null) {
				return this.jdbcTemplate;
			}
			if (this.dataSource != null) {
				return new JdbcTemplate(this.dataSource);
			}
			throw new IllegalArgumentException("DataSource must be set (either via dataSource() or jdbcTemplate())");
		}

		private DataSource resolveDataSource() {
			if (this.dataSource != null) {
				return this.dataSource;
			}
			if (this.jdbcTemplate != null && this.jdbcTemplate.getDataSource() != null) {
				return this.jdbcTemplate.getDataSource();
			}
			throw new IllegalArgumentException("DataSource must be set (either via dataSource() or jdbcTemplate())");
		}

		private JdbcChatMemoryRepositoryDialect resolveDialect(DataSource dataSource) {
			if (this.dialect == null) {
				try {
					return JdbcChatMemoryRepositoryDialect.from(dataSource);
				} catch (Exception ex) {
					throw new IllegalStateException("Could not detect dialect from datasource", ex);
				}
			} else {
				warnIfDialectMismatch(dataSource, this.dialect);
				return this.dialect;
			}
		}

		/**
		 * Logs a warning if the explicitly set dialect differs from the dialect detected
		 * from the DataSource.
		 */
		private void warnIfDialectMismatch(DataSource dataSource, JdbcChatMemoryRepositoryDialect explicitDialect) {
			try {
				JdbcChatMemoryRepositoryDialect detected = JdbcChatMemoryRepositoryDialect.from(dataSource);
				if (!detected.getClass().equals(explicitDialect.getClass())) {
					logger.warn("Explicitly set dialect {} will be used instead of detected dialect {} from datasource",
							explicitDialect.getClass().getSimpleName(), detected.getClass().getSimpleName());
				}
			} catch (Exception ex) {
				logger.debug("Could not detect dialect from datasource", ex);
			}
		}

	}
}
