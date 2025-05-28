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
}
