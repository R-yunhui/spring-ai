package com.ral.young.spring.ai;

import com.ral.young.spring.ai.config.CustomOpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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
