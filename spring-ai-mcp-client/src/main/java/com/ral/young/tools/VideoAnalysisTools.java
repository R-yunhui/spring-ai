package com.ral.young.tools;

import com.ral.young.utils.VideoDataUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.ral.young.utils.VideoDataUtils.executeAlgorithm;
import static com.ral.young.utils.VideoDataUtils.generateAnalysisSummary;
import static com.ral.young.utils.VideoDataUtils.generateCustomAlgorithm;
import static com.ral.young.utils.VideoDataUtils.matchBuiltinAlgorithms;

/**
 * @author renyh
 * @description 视频分析相关工具
 * @date 2025/7/1 10:00
 * @since 1.0.0
 */
@Service
@Slf4j
@SuppressWarnings("preview")
public class VideoAnalysisTools {

	/**
	 * 视频分析工具 - CV模型分析
	 * 使用专业CV模型分析一个或多个视频的内容，检测特定事件或行为
	 *
	 * @param videoIds     要分析的视频ID列表，可以是单个ID或多个ID
	 * @param analysisType 分析类型，如"事件检测"、"行为分析"等
	 * @param eventTypes   要检测的事件类型列表，如["人员入侵", "车辆入侵", "烟火烟雾"]
	 * @return 分析结果
	 */
	@Tool(description = "使用专业CV模型分析视频内容，检测特定事件或行为。精确度高但处理时间较长。返回的分析结果可直接用于生成分析报告。")
	public Map<String, Object> analyzeVideos(
			@ToolParam(description = "要分析的视频ID或ID列表") List<String> videoIds,
			@ToolParam(description = "分析类型，如'事件检测'、'行为分析'等") String analysisType,
			@ToolParam(description = "要检测的事件类型列表，如['人员入侵', '车辆入侵', '烟火烟雾']") List<String> eventTypes
			) {

		log.info("调用视频分析工具，视频数量: {}, 分析类型: {}, 事件类型: {}",
				videoIds.size(), analysisType, eventTypes);

		// 步骤1: 匹配内置算法
		Map<String, Object> matchResult = matchBuiltinAlgorithms(analysisType, eventTypes);
		List<Map<String, Object>> matchedAlgorithms = (List<Map<String, Object>>) matchResult.get("algorithms");

		// 步骤2: 如果没有匹配到内置算法，尝试生成自定义算法
		List<Map<String, Object>> customAlgorithms = new ArrayList<>();
		if (matchedAlgorithms.isEmpty() && eventTypes != null && !eventTypes.isEmpty()) {
			Map<String, Object> generatedResult = generateCustomAlgorithm(
					STR."分析视频中的\{String.join("、", eventTypes)}",
					eventTypes,
					null
			);

			if ((boolean) generatedResult.get("success")) {
				customAlgorithms.add((Map<String, Object>) generatedResult.get("algorithm"));
			}
		}

		// 步骤3: 执行算法分析
		List<Map<String, Object>> analysisResults = new ArrayList<>();

		// 首先尝试使用内置算法
		if (!matchedAlgorithms.isEmpty()) {
			for (String videoId : videoIds) {
				Map<String, Object> algorithm = matchedAlgorithms.getFirst();
				Map<String, Object> result = executeAlgorithm(
						(String) algorithm.get("id"),
						videoId
				);
				result.put("videoId", videoId);
				result.put("algorithmType", "builtin");
				analysisResults.add(result);
			}
		}
		// 如果没有内置算法但有自定义算法，使用自定义算法
		else if (!customAlgorithms.isEmpty()) {
			for (String videoId : videoIds) {
				Map<String, Object> algorithm = customAlgorithms.getFirst();
				Map<String, Object> result = executeAlgorithm(
						(String) algorithm.get("id"),
						videoId
				);
				result.put("videoId", videoId);
				result.put("algorithmType", "custom");
				analysisResults.add(result);
			}
		}
		// 如果两种算法都没有，使用视频切片分析
		else {
			for (String videoId : videoIds) {
				String queryText = "检测视频中的";
				if (eventTypes != null && !eventTypes.isEmpty()) {
					queryText += String.join("、", eventTypes);
				} else {
					queryText += "异常事件";
				}

				Map<String, Object> result = analyzeVideoBySlicing(videoId, queryText);
				result.put("videoId", videoId);
				result.put("algorithmType", "llm");
				analysisResults.add(result);
			}
		}

		// 汇总分析结果
		Map<String, Object> finalResult = new HashMap<>();
		finalResult.put("analysisType", analysisType);
		finalResult.put("eventTypes", eventTypes);
		finalResult.put("videoCount", videoIds.size());
		finalResult.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		finalResult.put("results", analysisResults);
		finalResult.put("summary", generateAnalysisSummary(analysisResults, analysisType, eventTypes));

		return finalResult;
	}

	/**
	 * 视频大模型分析工具
	 * 使用大模型分析视频内容
	 *
	 * @param videoId 视频ID
	 * @param query   分析问题
	 * @return 分析结果
	 */
	@Tool(description = "使用大模型对视频进行分析。处理速度较快但精确度可能略低。返回的分析结果可直接用于生成分析报告。")
	public Map<String, Object> analyzeVideoBySlicing(
			@ToolParam(description = "视频ID") String videoId,
			@ToolParam(description = "分析问题") String query
	) {

		int interval = 30; // 默认切片间隔
		log.info("调用视频切片分析工具，视频ID: {}, 查询: {}, 切片间隔: {}秒",
				videoId, query, interval);

		// 模拟视频切片和分析过程
		Map<String, Object> videoInfo = VideoDataUtils.getVideoById(videoId);
		if (videoInfo == null) {
			// 如果找不到视频，返回空结果
			Map<String, Object> emptyResult = new HashMap<>();
			emptyResult.put("videoId", videoId);
			emptyResult.put("success", false);
			emptyResult.put("error", "找不到指定ID的视频");
			return emptyResult;
		}

		int duration = (int) videoInfo.get("duration");
		int sliceCount = (duration / interval) + (duration % interval > 0 ? 1 : 0);

		// 生成切片分析结果
		List<Map<String, Object>> sliceResults = new ArrayList<>();
		boolean eventDetected = false;
		Map<String, Object> detectedEvent = null;

		// 根据视频类型和查询内容生成不同的分析结果
		String description = (String) videoInfo.get("description");

		if (query.contains("入侵") && (description.contains("尝试") || description.contains("陌生人"))) {
			// 人员入侵事件
			eventDetected = true;
			detectedEvent = Map.of(
					"eventType", "人员入侵",
					"confidence", 0.92,
					"timestamp", interval * 2,
					"description", "检测到未授权人员进入受限区域",
					"location", "视频画面右下角",
					"severity", "高"
			);
		} else if (query.contains("烟") && description.contains("烟雾")) {
			// 烟雾事件
			eventDetected = true;
			detectedEvent = Map.of(
					"eventType", "烟雾检测",
					"confidence", 0.95,
					"timestamp", interval,
					"description", "检测到明显烟雾",
					"location", "视频画面中央",
					"severity", "高"
			);
		} else if (query.contains("异常") && description.contains("徘徊")) {
			// 异常行为
			eventDetected = true;
			detectedEvent = Map.of(
					"eventType", "异常行为",
					"confidence", 0.88,
					"timestamp", interval * 1.5,
					"description", "检测到可疑徘徊行为",
					"location", "视频画面左侧",
					"severity", "中"
			);
		} else if (query.contains("人物") && description.contains("男")) {
			// 人物检测
			eventDetected = true;
			detectedEvent = Map.of(
					"eventType", "人物检测",
					"confidence", 0.94,
					"timestamp", interval,
					"description", "检测到符合描述的人物",
					"location", "视频画面中央",
					"severity", "低"
			);
		}

		// 添加检测到的事件
		List<Map<String, Object>> events = new ArrayList<>();
		if (eventDetected && detectedEvent != null) {
			events.add(detectedEvent);

			// 如果是安全相关事件，可能添加后续事件
			if (detectedEvent.get("eventType").equals("人员入侵") ||
					detectedEvent.get("eventType").equals("异常行为")) {

				// 添加后续事件
				events.add(Map.of(
						"eventType", "活动跟踪",
						"confidence", 0.85,
						"timestamp", (int) detectedEvent.get("timestamp") + interval,
						"description", "目标继续在区域内活动",
						"location", "视频画面中央",
						"severity", detectedEvent.get("severity")
				));
			}

			// 如果是烟雾检测，可能添加火灾警报
			if (detectedEvent.get("eventType").equals("烟雾检测") && Math.random() > 0.5) {
				events.add(Map.of(
						"eventType", "火灾警报",
						"confidence", 0.82,
						"timestamp", (int) detectedEvent.get("timestamp") + interval * 2,
						"description", "烟雾触发火灾警报",
						"location", "整个区域",
						"severity", "高"
				));
			}
		}

		// 创建分析结果
		Map<String, Object> result = new HashMap<>();
		result.put("videoId", videoId);
		result.put("query", query);
		result.put("sliceCount", sliceCount);
		result.put("sliceInterval", interval);
		result.put("events", events);
		result.put("success", true);

		// 添加结论
		if (!events.isEmpty()) {
			String severity = (String) events.getFirst().get("severity");
			String eventType = (String) events.getFirst().get("eventType");

			if (severity.equals("高")) {
				result.put("conclusion", STR."检测到高风险\{eventType}，建议立即处理");
			} else if (severity.equals("中")) {
				result.put("conclusion", STR."检测到中等风险\{eventType}，建议尽快关注");
			} else {
				result.put("conclusion", STR."检测到\{eventType}，风险较低");
			}
		} else {
			result.put("conclusion", "未检测到相关事件");
		}

		return result;
	}
}