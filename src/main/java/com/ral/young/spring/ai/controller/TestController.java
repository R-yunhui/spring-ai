package com.ral.young.spring.ai.controller;

import com.ral.young.spring.ai.service.SqlService;
import com.ral.young.spring.ai.service.WeatherService;
import jakarta.annotation.Generated;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.Generation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * @author renyh
 * @description 测试接口
 * @date 2025/4/14 11:14
 * @since 1.0.0
 */
@RestController
@RequestMapping(value = "/test")
public class TestController {

	@Resource
	private WeatherService weatherService;
	@Resource
	private SqlService sqlService;

	@RequestMapping(value = "/tool")
	public String testTool(@RequestParam(value = "prompt") String prompt) {
		return weatherService.testTool(prompt);
	}

	@RequestMapping(value = "/sql")
	public Object testSql(@RequestParam(value = "prompt") String prompt) throws IOException {
		return sqlService.testSql(prompt);
	}

	@RequestMapping(value = "/chat", produces = "text/event-stream")
	public Flux<Generation> testChat(@RequestParam(value = "prompt") String prompt) {
		return weatherService.testChat(prompt);
	}

	@RequestMapping(value = "/deepSeek")
	public String testDeepSeek(@RequestParam(value = "prompt") String prompt) {
		return weatherService.testDeepSeek(prompt);
	}
}
