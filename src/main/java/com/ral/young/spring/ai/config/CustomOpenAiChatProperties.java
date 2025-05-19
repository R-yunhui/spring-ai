package com.ral.young.spring.ai.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.autoconfigure.openai.OpenAiChatProperties;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author renyh
 * @description 模型配置文件
 * @date 2025/4/10 19:05
 * @since 1.0.0
 */
@Setter
@Getter
@ConfigurationProperties(value = CustomOpenAiChatProperties.CONFIG_PREFIX)
public class CustomOpenAiChatProperties extends OpenAiChatProperties {

	public static final String CONFIG_PREFIX = "spring.ai.openai.chat";

	public static final String DEFAULT_CHAT_MODEL = "gpt-4";

	public static final String DEFAULT_COMPLETIONS_PATH = "/v1/chat/completions";

	private static final Double DEFAULT_TEMPERATURE = 0.7;

	/**
	 * Enable OpenAI chat model.
	 */
	private boolean enabled = true;

	// 配置多个模型的列表
	private List<OpenAiChatModelConfig> models = new ArrayList<>();

	// 定义一个内部类来存储每个模型的配置
	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OpenAiChatModelConfig {
		private String name;
		private String baseUrl;
		private String appKey;
		private String completionsPath = DEFAULT_COMPLETIONS_PATH;

		private OpenAiChatOptions options = OpenAiChatOptions.builder()
				.model(DEFAULT_CHAT_MODEL)
				.temperature(DEFAULT_TEMPERATURE)
				.build();
	}
}

