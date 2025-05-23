package com.ral.young.spring.ai.service;

import cn.hutool.core.date.DateUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @author renyh
 * @description 模拟 function call 服务
 * @date 2025/5/23 16:24
 * @since 1.0.0
 */
public class ToolService {

	@Tool(name = "getCurrentTime", description = "获取当前时间")
	public String getCurrentTime() {
		return DateUtil.date().toStringDefaultTimeZone();
	}

	@Tool(name = "getCurrentTimeWithCity", description = "获取指定城市的当前时间")
	public String getCurrentTime(@ToolParam(description = "指定的城市名称") String city) {
		return switch (city) {
			case "北京" -> DateUtil.date().toStringDefaultTimeZone();
			case "上海" -> DateUtil.offsetDay(DateUtil.date(), 1).toStringDefaultTimeZone();
			case "广州" -> DateUtil.offsetDay(DateUtil.date(), 3).toStringDefaultTimeZone();
			default -> DateUtil.offsetDay(DateUtil.date(), 5).toStringDefaultTimeZone();
		};
	}
}
