package com.ral.young.spring.ai.controller;

import com.ral.young.spring.ai.dto.EmbeddingDTO;
import com.ral.young.spring.ai.dto.ImageDTO;
import com.ral.young.spring.ai.entity.CustomEmbeddingResponse;
import com.ral.young.spring.ai.service.OpenAiService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author Admin
 * @description TODO
 * @date 2025/5/22 14:30
 * @since 1.0.0
 */
@RestController
@RequestMapping(value = "/api/v1")
public class OpenAiController {

	@Resource
	private OpenAiService openAiService;

	@GetMapping(value = "/open-ai/chat")
	public String chat(@RequestParam(value = "prompt") String prompt) {
		return openAiService.chat(prompt);
	}

	@GetMapping(value = "/deepseek/chat")
	public String deepSeek(@RequestParam(value = "prompt") String prompt) {
		return openAiService.deepSeek(prompt);
	}

	@GetMapping(value = "/open-ai/test-tool")
	public String testTool(@RequestParam(value = "prompt") String prompt) {
		return openAiService.testTool(prompt);
	}

	@GetMapping(value = "/open-ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chatStream(@RequestParam(value = "prompt") String prompt) {
		return openAiService.chatStream(prompt);
	}

	@PostMapping(value = "/open-ai/chat-with-image")
	public String chatWithImage(@RequestBody ImageDTO imageDTO) {
		return openAiService.chatWithImage(imageDTO);
	}

	@PostMapping(value = "/open-ai/embedding")
	public CustomEmbeddingResponse embedding(@RequestBody List<EmbeddingDTO> embeddingDTOList) {
		return openAiService.embedding(embeddingDTOList);
	}
}
