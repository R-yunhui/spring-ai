package com.ral.young.spring.ai.controller;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author renyh
 * @description sse
 * @date 2025/4/28 13:46
 * @since 1.0.0
 */
@RestController
@RequestMapping("/sse")
public class SseController {

	@GetMapping(path = "/stream-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamEvents() {
		// 使用 AtomicLong 来安全地生成事件 ID
		AtomicLong eventIdCounter = new AtomicLong();

		// 每隔 1 秒生成一个事件
		return Flux.interval(Duration.ofSeconds(1))
				.map(sequence -> ServerSentEvent.<String>builder()
						.id(String.valueOf(eventIdCounter.incrementAndGet())) // 设置事件 ID
						.event("message") // 设置事件类型 (可选, 默认是 'message')
						.data("服务器时间: " + LocalTime.now().toString()) // 设置事件数据
						.comment("这是一个注释行，客户端会忽略") // 添加注释 (可选)
						.retry(Duration.ofSeconds(5)) // 建议客户端在断开连接后 5 秒重试 (可选)
						.build());
	}

	// 你也可以定义发送不同类型事件的端点
	@GetMapping(path = "/stream-custom-event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<String>> streamCustomEvents() {
		AtomicLong eventIdCounter = new AtomicLong();
		return Flux.interval(Duration.ofSeconds(2))
				.map(sequence -> ServerSentEvent.<String>builder()
						.id(String.valueOf(eventIdCounter.incrementAndGet()))
						.event("custom-event") // 自定义事件类型
						.data("{\"value\": " + sequence + ", \"timestamp\": \"" + LocalTime.now() + "\"}") // 发送 JSON 格式数据
						.build());
	}
}