package com.ral.young.spring.ai.config;

import com.ral.young.spring.ai.constant.CommonConstant;
import com.ral.young.spring.ai.model.CustomDocumentEmbeddingModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author renyh
 * @description 自定义配置模型
 * @date 2025/5/23 10:52
 * @since 1.0.0
 */

@Configuration
@EnableConfigurationProperties(CustomOpenAiProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.openai.custom", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CustomOpenAiAutoConfiguration {

	@Resource
	private CustomOpenAiProperties customOpenAiProperties;
	@Resource
	private ConfigurableListableBeanFactory beanFactory;
	@Resource
	private WebClient.Builder webClientBuilder;
	@Resource
	private RestClient.Builder restClientBuilder;

	// 动态注册每个启用的 CHAT 或 IMAGE_GENERATION 模型为独立 Bean
    // 动态注册每个启用的 EMBEDDING 模型为独立 Bean
	@PostConstruct
	public void registerModels() {
		customOpenAiProperties.getModels().stream()
				.filter(config -> config.getEnable() && (config.getModelType() == CommonConstant.ModelType.CHAT || config.getModelType() == CommonConstant.ModelType.IMAGE_GENERATION))
				.forEach(config -> {
					String beanName = config.getName(); // 使用 model 字段作为 Bean 名称
					OpenAiChatModel model = createOpenAiChatModel(config);
					beanFactory.registerSingleton(beanName, model);
				});

        customOpenAiProperties.getModels().stream()
                .filter(config -> config.getEnable() && config.getModelType() == CommonConstant.ModelType.EMBEDDING)
                .forEach(config -> {
                    String beanName = config.getName(); // 使用 model 字段作为 Bean 名称
                    CustomDocumentEmbeddingModel model = createEmbeddingModel(config);
                    beanFactory.registerSingleton(beanName, model);
                });
	}

	private OpenAiChatModel createOpenAiChatModel(CustomOpenAiProperties.OpenAiChatModelConfig config) {
		OpenAiApi openAiApi = OpenAiApi.builder()
				.baseUrl(config.getBaseUrl())
				.apiKey(config.getAppKey())
				.completionsPath(config.getCompletionsPath())
				.webClientBuilder(webClientBuilder)
				.restClientBuilder(restClientBuilder)
				.build();

		return OpenAiChatModel.builder()
				.openAiApi(openAiApi)
				.defaultOptions(config.getOptions())
				.build();
	}

	private CustomDocumentEmbeddingModel createEmbeddingModel(CustomOpenAiProperties.OpenAiChatModelConfig config) {
		CustomOpenAiApi openAiApi = CustomOpenAiApi.builder(new SimpleApiKey(config.getAppKey()))
				.baseUrl(config.getBaseUrl())
				.apiKey(config.getAppKey())
				.embeddingsPath(config.getEmbeddingsPath())
				.webClientBuilder(webClientBuilder)
				.restClientBuilder(restClientBuilder)
				.build();

		return new CustomDocumentEmbeddingModel(openAiApi, config.getOptions());
	}
}
