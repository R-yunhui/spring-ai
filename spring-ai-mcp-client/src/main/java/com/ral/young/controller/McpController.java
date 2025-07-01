package com.ral.young.controller;

import com.ral.young.service.McpService;
import com.ral.young.vo.ChatRequestVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping("/chat/")
	public String chat(@RequestBody ChatRequestVO chatRequestVO) {
		return mcpService.chatWithMcp(chatRequestVO);
	}

	@GetMapping("/tools")
	public String tools() {
		return mcpService.getMcpTools();
	}
}
