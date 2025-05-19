package com.ral.young.spring.ai.tools;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author renyh
 * @description 天气函数
 * @date 2025/4/14 11:13
 * @since 1.0.0
 */
@Component
@Slf4j
public class WeatherTools {

	private final WebClient webClient;

	private static final String WEATHER_API_URL = "https://api.weatherapi.com/v1/forecast.json";

	public WeatherTools() {
		this.webClient = WebClient.builder()
				.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
				.defaultHeader("key", "3442fc349f2c4ca496362130252204")
				.build();
	}

	@Tool(description = "通过这个方法可以获取到指定城市指定预报的天数的天气信息")
	public Response getWeatherServiceMethod(@ToolParam(description = "城市的英文名称，比如：beijing，chengdu，paris等") String city,
											@ToolParam(description = "天气预报天数。数值范围从1到14") int days) {

		if (!StringUtils.hasText(city)) {
			log.error("Invalid request: city is required.");
			return null;
		}
		String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
				.queryParam("q", city)
				.queryParam("days", days)
				.toUriString();
		log.info("url : {}", url);
		try {
			Mono<String> responseMono = webClient.get().uri(url).retrieve().bodyToMono(String.class);
			String jsonResponse = responseMono.block();
			assert jsonResponse != null;
			Response response = fromJson(JSONUtil.parseObj(jsonResponse));
			log.info("获取：{} 的天气信息成功，天气信息: {}", response.city(), response.current());
			return response;
		} catch (Exception e) {
			log.error("Failed to fetch weather data: {}", e.getMessage());
			return null;
		}
	}

	public static Response fromJson(JSONObject jsonObject) {
		Map<String, Object> location = (Map<String, Object>) jsonObject.get("location");
		Map<String, Object> current = (Map<String, Object>) jsonObject.get("current");
		Map<String, Object> forecast = (Map<String, Object>) jsonObject.get("forecast");
		List<Map<String, Object>> forecastDays = (List<Map<String, Object>>) forecast.get("forecastday");
		String city = (String) location.get("name");
		return new Response(city, current, forecastDays);
	}

	public record Response(String city, Map<String, Object> current, List<Map<String, Object>> forecastDays) {
	}
}
