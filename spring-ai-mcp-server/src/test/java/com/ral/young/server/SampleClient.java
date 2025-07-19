/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ral.young.server;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

import java.util.Map;

/**
 * @author renyh
 * @description 测试
 * @date 2025/7/19 10:01
 * @since 1.0.0
 */
@SuppressWarnings("preview")
public class SampleClient {

	private final McpClientTransport transport;

	public SampleClient(McpClientTransport transport) {
		this.transport = transport;
	}

	public void run() {

		var client = McpClient.sync(this.transport).build();

		client.initialize();

		client.ping();

		// 列出并展示可用的工具
		ListToolsResult toolsList = client.listTools();
		System.out.println(STR."可用工具 = \{toolsList}");

		// 获取北京的天气预报
		CallToolResult weatherForecastResult = client.callTool(new CallToolRequest("getWeatherForecastByLocation",
				Map.of("latitude", "39.9042", "longitude", "116.4074")));
		System.out.println(STR."北京天气预报: \{weatherForecastResult}");

		// 获取北京的空气质量信息
		CallToolResult airQualityResult = client.callTool(new CallToolRequest("getAirQuality",
				Map.of("latitude", "39.9042", "longitude", "116.4074")));
		System.out.println(STR."北京空气质量: \{airQualityResult}");

		client.closeGracefully();
	}
}
