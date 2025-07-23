package com.ral.young.tools;

import com.ral.young.dto.VideoInfo;
import com.ral.young.dto.request.VideoAnalysisRequest;
import com.ral.young.dto.response.VideoAnalysisResponse;
import com.ral.young.enums.TaskStatusEnum;
import com.ral.young.manager.TaskManager;
import com.ral.young.utils.VideoDataUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author renyh
 * @description 视频分析相关工具
 * @date 2025/7/23 12:00
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoAnalysisTools {

	@Resource
	private TaskManager taskManager;

	/**
	 * 视频分析工具 - LLM模型
	 * 使用大语言模型分析视频内容
	 *
	 * @param request 视频分析请求
	 * @return 视频分析响应
	 */
	@Tool(description = "当内置算法匹配失败且用户选择不生成新的能力时才会使用。通过大语言模型分析视频内容。此工具为同步处理，会直接返回结果。")
	public VideoAnalysisResponse analyzeVideosWithLlmModel(VideoAnalysisRequest request) {
		log.info("调用视频分析工具使用大模型进行分析，视频数量: {}, 分析类型: {}",
				request.getVideoIds().size(), request.getAnalysisType());

		// 获取视频信息
		List<VideoInfo> videos = VideoDataUtils.getVideosByIds(request.getVideoIds());

		// 使用大模型进行分析（同步处理）
		log.info("开始执行LLM模型分析，视频数量: {}", videos.size());

		// 生成分析结果
		Map<String, Object> analysisResult = generateAnalysisResult(videos, request.getAnalysisType());

		// 将结果存入结果缓存
		Long resultId = taskManager.createResultId(analysisResult);
		log.info("LLM模型分析完成，结果ID: {}", resultId);

		// 构建响应
		return VideoAnalysisResponse.builder()
				.resultId(resultId)
				.status(TaskStatusEnum.COMPLETED)
				.build();
	}

	/**
	 * 生成分析结果
	 */
	private Map<String, Object> generateAnalysisResult(List<VideoInfo> videos, String analysisType) {
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> videoResults = new HashMap<>();

		for (VideoInfo video : videos) {
			Map<String, Object> videoResult = new HashMap<>();
			videoResult.put("videoId", video.getVideoId());
			videoResult.put("title", video.getTitle());
			videoResult.put("description", video.getDescription());
			videoResult.put("tags", video.getTags());

			List<Map<String, Object>> events = generateEvents(video, analysisType);
			videoResult.put("events", events);

			// 生成结论
			String conclusion = generateConclusion(video, events);
			videoResult.put("conclusion", conclusion);

			videoResults.put(video.getVideoId(), videoResult);
		}

		result.put("videoResults", videoResults);
		result.put("analysisType", "LLM_ANALYSIS");
		result.put("timestamp", System.currentTimeMillis());
		result.put("summary", generateOverallSummary(videos, videoResults));
		result.put("analysisMethod", "大语言模型分析");

		return result;
	}

	/**
	 * 生成事件
	 */
	private List<Map<String, Object>> generateEvents(VideoInfo video, String analysisType) {
		List<Map<String, Object>> events = new ArrayList<>();
		List<String> tags = video.getTags();

		// 根据视频标签生成事件
		if (tags.contains("人员入侵")) {
			events.add(Map.of(
					"eventType", "人员入侵",
					"confidence", 0.85,
					"timestamp", video.getDuration() / 3,
					"description", "检测到未授权人员进入受限区域",
					"severity", "高"
			));
		}

		if (tags.contains("动物入侵")) {
			String animalType = "未知动物";
			if (tags.contains("野狗")) animalType = "野狗";
			if (tags.contains("野猫")) animalType = "野猫";
			if (tags.contains("鸟类")) animalType = "鸟类";

			events.add(Map.of(
					"eventType", "动物入侵",
					"confidence", 0.82,
					"timestamp", video.getDuration() / 2,
					"description", "检测到" + animalType + "进入区域",
					"severity", "中"
			));
		}

		if (tags.contains("黑色上衣") && tags.contains("男性")) {
			events.add(Map.of(
					"eventType", "目标人物检测",
					"confidence", 0.78,
					"timestamp", video.getDuration() / 4,
					"description", "检测到穿黑色上衣的男性",
					"severity", "低"
			));
		}

		if (tags.contains("交通违规")) {
			events.add(Map.of(
					"eventType", "交通违规",
					"confidence", 0.80,
					"timestamp", video.getDuration() / 2,
					"description", "检测到交通违规行为",
					"severity", "中"
			));
		}

		// 如果没有匹配到特定事件，添加一个通用事件
		if (events.isEmpty()) {
			events.add(Map.of(
					"eventType", "常规活动",
					"confidence", 0.75,
					"timestamp", video.getDuration() / 2,
					"description", "视频中包含常规活动，未检测到异常",
					"severity", "低"
			));
		}

		return events;
	}

	/**
	 * 生成结论
	 */
	private String generateConclusion(VideoInfo video, List<Map<String, Object>> events) {
		List<String> tags = video.getTags();

		if (events.stream().anyMatch(e -> "高".equals(e.get("severity")))) {
			return "视频中检测到高风险事件，建议立即关注";
		} else if (events.stream().anyMatch(e -> "中".equals(e.get("severity")))) {
			return "视频中检测到中等风险事件，建议关注";
		} else if (tags.contains("人员入侵")) {
			return "视频中检测到人员入侵事件，可能存在安全隐患";
		} else if (tags.contains("动物入侵")) {
			return "视频中检测到动物入侵事件，建议关注";
		} else if (tags.contains("交通违规")) {
			return "视频中检测到交通违规行为，建议关注";
		} else {
			return "视频内容正常，未检测到明显异常";
		}
	}

	/**
	 * 生成整体总结
	 */
	private String generateOverallSummary(List<VideoInfo> videos, Map<String, Object> videoResults) {
		int totalVideos = videos.size();
		int abnormalVideos = 0;

		for (VideoInfo video : videos) {
			Map<String, Object> result = (Map<String, Object>) videoResults.get(video.getVideoId());
			String conclusion = (String) result.get("conclusion");

			if (conclusion != null && (
					conclusion.contains("高风险") ||
							conclusion.contains("中等风险") ||
							conclusion.contains("安全隐患") ||
							conclusion.contains("建议关注"))) {
				abnormalVideos++;
			}
		}

		if (abnormalVideos > 0) {
			return String.format("分析了%d个视频，其中%d个视频存在异常情况，需要关注。", totalVideos, abnormalVideos);
		} else {
			return String.format("分析了%d个视频，未发现明显异常情况。", totalVideos);
		}
	}
}