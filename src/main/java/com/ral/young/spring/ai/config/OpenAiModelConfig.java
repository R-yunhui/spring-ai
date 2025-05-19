package com.ral.young.spring.ai.config;

import com.ral.young.spring.ai.model.CustomEmbeddingModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author renyh
 * @description 配置多个不同地址的 chatmodel
 * @date 2025/4/10 19:13
 * @since 1.0.0
 */
@Configuration
public class OpenAiModelConfig {

	private final CustomOpenAiChatProperties properties;

	public OpenAiModelConfig(CustomOpenAiChatProperties properties) {
		this.properties = properties;
	}

	@Bean
	public OpenAiChatModel qwen72BModel() {
		return properties.getModels().stream()
				.filter(config -> "qwen-72b".equals(config.getOptions().getModel()))
				.findFirst()
				.map(this::createOpenAiChatModel)
				.orElseThrow(() -> new IllegalStateException("未找到 qwen2Model 配置"));
	}

	@Bean
	public OpenAiChatModel deepSeekModel() {
		return properties.getModels().stream()
				.filter(config -> "deepseek-r1".equals(config.getOptions().getModel()))
				.findFirst()
				.map(this::createOpenAiChatModel)
				.orElseThrow(() -> new IllegalStateException("未找到 deepSeekModel 配置"));
	}

	@Bean
	public CustomEmbeddingModel customEmbeddingModel() {
		return properties.getModels().stream()
				.filter(config -> "multimodal-embedding".equals(config.getOptions().getModel()))
				.findFirst()
				.map(this::createEmbeddingModel)
				.orElseThrow(() -> new IllegalStateException("未找到 embedding model 配置"));
	}

	private OpenAiChatModel createOpenAiChatModel(CustomOpenAiChatProperties.OpenAiChatModelConfig modelConfig) {
		OpenAiApi openAiApi = OpenAiApi.builder()
				.baseUrl(modelConfig.getBaseUrl())
				.apiKey(modelConfig.getAppKey())
				.completionsPath(modelConfig.getCompletionsPath())
				.build();

		return OpenAiChatModel.builder()
				.openAiApi(openAiApi)
				.defaultOptions(modelConfig.getOptions())
				.build();
	}

	public CustomEmbeddingModel createEmbeddingModel(CustomOpenAiChatProperties.OpenAiChatModelConfig modelConfig) {
		CustomOpenAiApi openAiApi = CustomOpenAiApi.builder(new SimpleApiKey(modelConfig.getAppKey()))
				.baseUrl(modelConfig.getBaseUrl())
				.apiKey(modelConfig.getAppKey())
				.completionsPath(modelConfig.getCompletionsPath())
				.build();

		OpenAiEmbeddingOptions options = new OpenAiEmbeddingOptions();
		options.setModel(modelConfig.getOptions().getModel());

		return new CustomEmbeddingModel(openAiApi, options);
	}
}
