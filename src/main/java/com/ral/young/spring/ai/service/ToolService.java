package com.ral.young.spring.ai.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * @author renyh
 * @description 模拟 function call 服务
 * @date 2025/5/23 16:24
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolService {

	@Resource
	private TaskScheduler taskScheduler;

	@Getter
	@Setter
	private String city = "成都";

	@Tool(name = "getCurrentTime", description = "获取当前系统时间，返回格式为：yyyy-MM-dd HH:mm:ss。在需要处理相对时间（如'明天'、'下周'）时，建议先调用此函数获取基准时间。")
	public String getCurrentTime() {
		String stringDefaultTimeZone = DateUtil.date().toStringDefaultTimeZone();
		log.info("当前系统时间：{}", stringDefaultTimeZone);
		return stringDefaultTimeZone;
	}

	@Tool(name = "createPeriodScheduledTask", description = "创建一个周期性执行的定时任务，用于定期检测指定的标签信息。任务会按照指定的cron表达式定期执行，直到应用程序停止。")
	public void createPeriodScheduledTask(
			@ToolParam(description = "cron表达式，用于定义任务的执行周期。例如：'0/10 * * * * ?'表示每10秒执行一次，'0 0 12 * * ?'表示每天中午12点执行，'0 0 12 ? * MON'表示每周一中午12点执行") String cornExpression,
			@ToolParam(description = "需要检测的标签列表，例如：['标签1', '标签2']。这些标签将在每次任务执行时被检测") List<String> labels) {
		log.info("创建周期性的定时任务，corn表达式：{}，要检测的标签列表：{}", cornExpression, labels);
		try {
			ScheduledFuture<?> future = taskScheduler.schedule(() -> {
				log.info("开始执行周期性任务 - 检测标签: {}, 当前时间: {}", labels, DateUtil.now());
				// 这里可以添加实际的业务逻辑
				log.info("周期性任务执行完成 - 检测标签: {}", labels);
			}, new CronTrigger(cornExpression));
			
			if (future != null) {
				log.info("周期性任务创建成功，下次执行时间: {}", future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS));
			} else {
				log.error("周期性任务创建失败");
			}
		} catch (Exception e) {
			log.error("创建周期性任务时发生错误", e);
		}
	}

	@Tool(name = "createDelayTask", description = "创建一个延迟执行的任务，用于在指定的时间点检测标签信息。注意：在使用此工具之前，必须先调用 getArea 获取当前用户所在的城市，以这个结果为主，再调用 getCurrentTime 获取当前时间作为基准，特别是当用户使用相对时间（如'明天'、'下周'）时。")
	public void createDelayTask(
			@ToolParam(description = "用户所在的城市，只接受 getArea 返回的结果。") String city,
			@ToolParam(description = "任务执行的具体时间点，只接受 getCurrentTime 函数返回的结果。格式为：yyyy-MM-dd HH:mm:ss。例如：'2025-06-12 15:30:00'。注意：如果使用相对时间，必须先调用 getCurrentTime 获取当前时间作为基准进行转换") String date,
			@ToolParam(description = "需要检测的标签列表，例如：['标签1', '标签2']。这些标签将在指定时间点被检测") List<String> labels) {
		if (!StrUtil.equals(city, "成都")) {
			throw new RuntimeException("条件不满足，任务将被忽略，无法执行");
		}

		log.info("创建延时执行任务，待执行时间：{}，要检测的标签列表：{}", date, labels);
		try {
			DateTime dateTime = DateUtil.parse(date, DatePattern.NORM_DATETIME_PATTERN);
			ScheduledFuture<?> future = taskScheduler.schedule(() -> {
				log.info("开始执行延时任务 - 检测标签: {}, 当前时间: {}", labels, DateUtil.now());
				// 这里可以添加实际的业务逻辑
				log.info("延时任务执行完成 - 检测标签: {}", labels);
			}, dateTime.toInstant());

			log.info("延时任务创建成功，将在 {} 后执行", future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS));
		} catch (Exception e) {
			log.error("创建延时任务时发生错误", e);
		}
	}

	@Tool(name = "getArea", description = "获取当前用户所在的地区。返回值包括：北京、上海、广州、其他")
	public String getArea() {
		return city;
	}

	@Tool(name = "getCurrentTimeWithCity", description = "获取指定城市的当前时间。支持的城市包括：北京（当前时间）、上海（当前时间+1天）、广州（当前时间+3天）、其他城市（当前时间+5天）")
	public String getCurrentTime(@ToolParam(description = "城市名称，目前支持：北京、上海、广州，其他城市将返回默认时间") String city) {
		return switch (city) {
			case "北京" -> DateUtil.date().toStringDefaultTimeZone();
			case "上海" -> DateUtil.offsetDay(DateUtil.date(), 1).toStringDefaultTimeZone();
			case "广州" -> DateUtil.offsetDay(DateUtil.date(), 3).toStringDefaultTimeZone();
			default -> DateUtil.offsetDay(DateUtil.date(), 5).toStringDefaultTimeZone();
		};
	}
}
