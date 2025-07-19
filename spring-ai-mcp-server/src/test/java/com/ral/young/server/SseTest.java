package com.ral.young.server;

import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author renyh
 * @description 测试 sse 连接 mcp server
 * @date 2025/7/19 10:01
 * @since 1.0.0
 */
public class SseTest {

	public static void main(String[] args) {
		var transport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("http://localhost:8080"));
		new SampleClient(transport).run();
	}
}
