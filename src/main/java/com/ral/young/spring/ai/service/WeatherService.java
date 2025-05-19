package com.ral.young.spring.ai.service;

import com.ral.young.spring.ai.tools.DateTimeTools;
import com.ral.young.spring.ai.tools.WeatherTools;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;

/**
 * @author renyh
 * @description 天气
 * @date 2025/4/14 10:59
 * @since 1.0.0
 */
@Service
@DependsOn(value = {"openAiModelConfig"})
@Slf4j
public class WeatherService {

	@Resource
	private OpenAiChatModel qwen72BModel;

	@Resource
	private OpenAiChatModel deepSeekModel;

	private ChatClient qwen72BChatClient;

	private ChatClient deepSeekChatClient;

	@PostConstruct
	public void init() {
		qwen72BChatClient = ChatClient.builder(qwen72BModel).build();
		deepSeekChatClient = ChatClient.builder(deepSeekModel).build();
	}

	public String testTool(String prompt) {
		String content = qwen72BChatClient.prompt(prompt).tools(new DateTimeTools(), new WeatherTools())
				.call()
				.content();
		log.info(content);
		return content;
	}

	public Flux<Generation> testChat(String prompt) {
		StringBuilder build = new StringBuilder();
		return qwen72BChatClient.prompt(prompt + "，请用中文回答，字数控制在150字左右")
				.stream()
				.chatResponse()
				.map(ChatResponse::getResult)
				.doOnNext(result -> {
					build.append(result.getOutput().getText());
				})
				.onErrorResume(error -> {
					// 记录错误日志
					log.error("流式聊天请求出错", error);
					// 返回一个空的 Flux 或者根据需求处理错误
					return Flux.empty();
				})
				.doOnComplete(() -> {
					// 输出完整的结果
					log.info("完整的输出结果：{}", build);
				});
	}

	public String testDeepSeek(String prompt) {
		ChatResponse chatResponse = deepSeekChatClient.prompt(prompt + "，请用中文回答，输出对应的推理过程，结果控制在500字左右。")
				.call()
				.chatResponse();
		String content = Optional.ofNullable(chatResponse)
				.map(ChatResponse::getResult)
				.map(Generation::getOutput)
				.map(AbstractMessage::getText)
				.orElse(null);
		log.info("deepSeek result={}", content);
		return content;
	}
}
