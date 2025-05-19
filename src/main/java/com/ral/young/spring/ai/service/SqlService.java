package com.ral.young.spring.ai.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author renyh
 * @description L To Sql
 * @date 2025/4/22 16:27
 * @since 1.0.0
 */
@Service
@Slf4j
public class SqlService {

	@Resource
	private DashScopeChatModel dashScopeChatModel;
	@Resource
	private JdbcTemplate jdbcTemplate;

	@Value("classpath:/schema.sql")
	private org.springframework.core.io.Resource ddlResource;

	@Value("classpath:/schematwo.sql")
	private org.springframework.core.io.Resource ddlTwoResource;

	@Value("classpath:/sql-prompt-template.st")
	private org.springframework.core.io.Resource sqlPromptTemplateResource;

	private  ChatClient qwen72BChatClient;

	@PostConstruct
	public void init() {
		this.qwen72BChatClient = ChatClient.builder(dashScopeChatModel)
				.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
				.build();
	}

	public Object testSql(String prompt) throws IOException {
		String schema = ddlTwoResource.getContentAsString(Charset.defaultCharset());
		String content = sqlPromptTemplateResource.getContentAsString(Charset.defaultCharset());
		content = content.replace("{question}", prompt);
		content = content.replace("{ddl}", schema);
		String sql = qwen72BChatClient.prompt()
				.user(content)
				.call()
				.content();
		log.info("SQL prompt result: {}", sql);
		if (StrUtil.isNotBlank(sql)) {
			if (sql.contains("```sql")) {
				sql = sql.substring(sql.indexOf("```sql") + 6);
				sql = sql.substring(0, sql.indexOf("```"));
			}

			if (sql.toLowerCase().startsWith("select")) {
				return jdbcTemplate.queryForList(sql);
			}
		}
		return sql;
	}
}
