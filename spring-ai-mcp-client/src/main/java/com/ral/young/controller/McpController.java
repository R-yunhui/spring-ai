package com.ral.young.controller;

import com.ral.young.service.McpService;
import com.ral.young.service.VideoSearchService;
import com.ral.young.vo.ChatRequestVO;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author renyh
 * @description mcp 接口调用
 * @date 2025/6/30 19:44
 * @since 1.0.0
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

	@Resource
	private McpService mcpService;
	@Resource
	private VideoSearchService videoSearchService;

	@PostMapping("/chat/")
	public String chat(@RequestBody ChatRequestVO chatRequestVO) {
		return mcpService.chatWithMcp(chatRequestVO);
	}

	@GetMapping("/test")
	public void test() {
		videoSearchService.testAws();
	}

	@PostMapping(value = "/video/search/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<VideoSearchService.CustomChatResponse> videoSearchStream(@RequestBody ChatRequestVO chatRequestVO) {
		return videoSearchService.videoSearchStream(chatRequestVO);
	}

	@PostMapping(value = "/video/search")
	public ChatResponse videoSearch(@RequestBody ChatRequestVO chatRequestVO) {
		return videoSearchService.videoSearch(chatRequestVO);
	}

	@GetMapping(value = "/video/notify/{taskId}/{chatId}")
	public void notifyTaskCompletion(@PathVariable Long taskId, @PathVariable Long chatId) {
		videoSearchService.notifyTaskCompletion(taskId, chatId);
	}

	@GetMapping("/tools")
	public String tools() {
		return mcpService.getMcpTools();
	}
}
