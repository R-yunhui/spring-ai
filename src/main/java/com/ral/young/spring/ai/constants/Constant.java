package com.ral.young.spring.ai.constants;

import lombok.Getter;

/**
 * @author renyh
 * @description 公共定义
 * @date 2025/4/14 15:19
 * @since 1.0.0
 */
public class Constant {

	@Getter
	public static enum WeatherEnum {

		SUNNY("晴"),
		CLOUDY("多云"),
		RAINY("雨"),
		THUNDERSTORM("雷"),
		SNOWY("雪"),
		FOGGY("雾"),
		HAZE("霾"),
		DRIZZLE("雨夹雪"),
		SMOKY("烟雾"),
		HAIL("冰雹"),
		UNKNOWN("未知");

		private final String description;

		private WeatherEnum(String description) {
			this.description = description;
		}
	}

}
