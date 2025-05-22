package com.ral.young.spring.ai;

import com.ral.young.spring.ai.config.CustomOpenAiApi;
import com.ral.young.spring.ai.model.CustomDocumentEmbeddingModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author renyunhui
 * @description 启动类
 * @date 2025-02-14 09-31-24
 * @since 1.0.0
 */
@SpringBootApplication
public class AiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiApplication.class, args);
	}

	@Bean
	public CustomDocumentEmbeddingModel embeddingModel() {
		CustomOpenAiApi openAiApi = CustomOpenAiApi.builder(new SimpleApiKey("sk-n497jeZAydpNiUEUnDg5P9VZ5ecjTC0Li2T5RfMkQhQmgcz4"))
				.baseUrl("http://192.168.2.54:9015")
				.apiKey("sk-n497jeZAydpNiUEUnDg5P9VZ5ecjTC0Li2T5RfMkQhQmgcz4")
				.completionsPath("/v1/embeddings")
				.build();

		return new CustomDocumentEmbeddingModel(
				openAiApi
		);
	}
}
