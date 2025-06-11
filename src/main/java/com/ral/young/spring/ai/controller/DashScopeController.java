package com.ral.young.spring.ai.controller;

import com.ral.young.spring.ai.service.DashScopeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author renyh
 * @description 阿里巴巴模型接口
 * @date 2025/6/10 14:14
 * @since 1.0.0
 */
@RestController
@RequestMapping(value = "/api/v1")
public class DashScopeController {

	@Resource
	private DashScopeService dashScopeService;

	@GetMapping(value = "/dashscope/test-tool")
	public String testTool(@RequestParam(value = "prompt") String prompt) {
		return dashScopeService.testTool(prompt);
	}
}
