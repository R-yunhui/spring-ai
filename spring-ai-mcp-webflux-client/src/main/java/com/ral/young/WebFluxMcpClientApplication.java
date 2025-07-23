package com.ral.young;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author renyh
 * @description 通过 webflux 的方式调用 mcp server
 * @date 2025/7/19 10:24
 * @since 1.0.0
 */
@SpringBootApplication(exclude =
		{org.springframework.ai.mcp.client.autoconfigure.SseHttpClientTransportAutoConfiguration.class}
)
@EnableScheduling
@Slf4j
public class WebFluxMcpClientApplication {

	public static void main(String[] args) {
		System.out.println("Hello world!");
		SpringApplication.run(WebFluxMcpClientApplication.class, args);
	}
}