package com.ral.young.spring.ai;

import cn.hutool.core.util.StrUtil;
import com.ral.young.spring.ai.config.CustomOpenAiProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * @author renyunhui
 * @description 启动类
 * @date 2025-02-14 09-31-24
 * @since 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties(CustomOpenAiProperties.class)
public class AiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiApplication.class, args);
	}

	static MultiValueMap<String, String> additionalHttpHeader = MultiValueMap.fromSingleValue(Map.of(
			"Connection", "keep-alive"
	));

	@Bean
	public WebClient.Builder webClientBuilder(ObjectProvider<WebClientCustomizer> customizerProvider) {
		WebClient.Builder builder = WebClient.builder();
		customizerProvider.orderedStream().forEach((customizer) -> customizer.customize(builder));
		builder.defaultHeaders(h -> h.addAll(additionalHttpHeader));
		return builder;
	}

	@Bean
	RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
		return restClientBuilderConfigurer.configure(RestClient.builder()
				.defaultHeaders(h -> h.addAll(additionalHttpHeader))
		);
	}
}
