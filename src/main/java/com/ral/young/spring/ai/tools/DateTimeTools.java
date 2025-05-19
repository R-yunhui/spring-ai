package com.ral.young.spring.ai.tools;

import com.ral.young.spring.ai.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author renyh
 * @description 时间func tool
 * @date 2025/4/14 14:10
 * @since 1.0.0
 */
@Slf4j
public class DateTimeTools {

	// 获取当前日期和时间的工具方法
	@Tool(description = "获取当前日期和时间")
	public String getCurrentDateTime() {
		log.info("获取当前日期和时间={}", LocaleContextHolder.getTimeZone().toZoneId());
		return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
	}

	// 设置闹钟的工具方法
	@Tool(description = "设置一个闹钟，传入一个时间字符串，格式为ISO-8601，例如：2025-04-14T14:10:00")
	public LocalDateTime setAlarm(String time) {
		// 将传入的时间字符串按照ISO-8601格式解析为LocalDateTime对象
		LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
		// 打印设置的闹钟时间
		log.info("闹钟设置的时间为={}", alarmTime);
		return alarmTime;
	}


	@Tool(description = "根据当前所在的城市先获取天气，然后在根据天气设置一个闹钟")
	public LocalDateTime setAlarmWithWeather(@ToolParam(description = "当前的日期和时间") String time, @ToolParam(description = "获取天气之后传入对应的枚举值") Constant.WeatherEnum weatherEnum, @ToolParam(description = "当前所在的城市") String city) {
		LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
		switch (weatherEnum) {
			case SUNNY -> alarmTime = alarmTime.plusMinutes(10);
			case CLOUDY -> alarmTime = alarmTime.plusMinutes(20);
			default -> alarmTime = alarmTime.plusMinutes(30);
		}
		log.info("当前城市：{}的天气是：{}, 闹钟设置的时间为={}", city, weatherEnum.getDescription(), alarmTime);
		return alarmTime;
	}
}
