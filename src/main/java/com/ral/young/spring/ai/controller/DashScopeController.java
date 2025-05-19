package com.ral.young.spring.ai.controller;

import com.ral.young.spring.ai.service.DashScopeService;
import jakarta.annotation.Resource;
import org.springframework.ai.embedding.Embedding;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Set;

/**
 * @author renyh
 * @description 百炼大模型测试的接口
 * @date 2025/4/23 15:15
 * @since 1.0.0
 */
@RestController
@RequestMapping(value = "/dashscope")
public class DashScopeController {

	@Resource
	private DashScopeService dashScopeService;

	@GetMapping(value = "/text-chat")
	public String textChat(@RequestParam(value = "prompt") String prompt) {
		return dashScopeService.textChat(prompt);
	}

	@GetMapping(value = "/text-image-chat")
	public String textImageChat(@RequestParam(value = "prompt") String prompt, @RequestParam String url) {
		return dashScopeService.textImageChat(prompt, url);
	}

	@GetMapping(value = "/image-chat")
	public Set<String> imageChat(@RequestParam(value = "prompt") String prompt) {
		return dashScopeService.imageChat(prompt);
	}

	@GetMapping(value = "/text-embedding")
	public Embedding textEmbedding(@RequestParam(value = "prompt") String prompt) {
		return dashScopeService.textEmbedding(prompt);
	}

	@GetMapping(value = "/text-stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> textStreamChat(@RequestParam(value = "prompt") String prompt) {
		return dashScopeService.textStreamChat(prompt);
	}
}
