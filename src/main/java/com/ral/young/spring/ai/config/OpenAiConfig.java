package com.ral.young.spring.ai.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * @author renyh
 * @description 自定义 restClient 和 webClient 配置，解决 http1.1 会升级到 http2.0 的问题
 * @date 2025/5/28 14:16
 * @since 1.0.0
 */
@Configuration
public class OpenAiConfig {

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
	public RestClient.Builder restClientBuilder(RestClientBuilderConfigurer restClientBuilderConfigurer) {
		return restClientBuilderConfigurer.configure(RestClient.builder()
				.defaultHeaders(h -> h.addAll(additionalHttpHeader))
		);
	}
}
