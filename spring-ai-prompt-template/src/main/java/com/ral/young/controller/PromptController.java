package com.ral.young.controller;

import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplate;
import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplateFactory;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author : huangzhen
 */

@RestController
@RequestMapping("/nacos")
@Slf4j
public class PromptController {

	private final ChatClient client;

	private final ConfigurablePromptTemplateFactory promptTemplateFactory;

	public PromptController(
			ChatModel chatModel,
			ConfigurablePromptTemplateFactory promptTemplateFactory
	) {

		this.client = ChatClient.builder(chatModel).build();
		this.promptTemplateFactory = promptTemplateFactory;
	}

	@GetMapping("/books")
	public Flux<String> generateJoke(
			@RequestParam(value = "author", required = false, defaultValue = "鲁迅") String authorName,
			HttpServletResponse response
	) {

		// 防止输出乱码
		response.setCharacterEncoding("UTF-8");

		// 使用 nacos 的 prompt tmpl 创建 prompt
		Prompt prompt;
		if (promptTemplateFactory.getTemplate(authorName) != null) {
			prompt = promptTemplateFactory.getTemplate(authorName)
					.create();
		} else {
			ConfigurablePromptTemplate template = promptTemplateFactory.create(
					"author",
					"please list the three most famous books by this {author}."
			);
			prompt = template
					.create(Map.of("author", authorName));
		}
		log.info("最终构建的 prompt 为：{}", prompt.getContents());

		return client.prompt(prompt)
				.stream()
				.content();
	}

}
