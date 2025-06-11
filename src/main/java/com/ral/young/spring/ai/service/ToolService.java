package com.ral.young.spring.ai.service;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author renyh
 * @description 模拟 function call 服务
 * @date 2025/5/23 16:24
 * @since 1.0.0
 */
@Slf4j
@Service
public class ToolService {

	@Tool(name = "getCurrentTime", description = "获取当前时间")
	public String getCurrentTime() {
		return DateUtil.date().toStringDefaultTimeZone();
	}

	@Tool(name = "createPeriodScheduledTask", description = "创建一个周期性的定时任务去检测指定的标签信息")
	public void createPeriodScheduledTask(@ToolParam(description = "定时任务的corn表达式") String cornExpression, @ToolParam(description = "要检测的标签列表") List<String> labels) {
		log.info("创建周期性的定时任务，corn表达式：{}，要检测的标签列表：{}", cornExpression, labels);
	}

	@Tool(name = "createDelayTask", description = "创建一个延迟性的任务去检测指定的标签信息")
	public void createDelayTask(@ToolParam(description = "具体需要延迟多久执行的毫秒数") Long delayTime, @ToolParam(description = "要检测的标签列表") List<String> labels) {
		log.info("创建延时执行的任务，待延迟时间：{} 毫秒，要检测的标签列表：{}", delayTime, labels);
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
