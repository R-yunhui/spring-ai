package com.ral.young.tools;

import com.ral.young.dto.VideoInfo;
import com.ral.young.dto.request.AlgorithmAnalysisRequest;
import com.ral.young.dto.response.AlgorithmAnalysisResponse;
import com.ral.young.enums.TaskStatusEnum;
import com.ral.young.manager.TaskManager;
import com.ral.young.utils.VideoDataUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author renyh
 * @description 算法分析工具
 * @date 2025/7/25 11:05
 * @since 1.0.0
 */
@Service
@Slf4j
public class AlgorithmAnalysisTools {

	@Resource
	private TaskManager taskManager;

	/**
	 * 算法分析下发工具
	 * 下发算法进行视频分析
	 *
	 * @param request 算法分析请求
	 * @return 算法分析响应
	 */
	@Tool(description = "下发算法进行视频分析，这是一个异步任务，会使用指定算法分析视频。")
	public AlgorithmAnalysisResponse dispatchAlgorithmAnalysis(AlgorithmAnalysisRequest request) {
		log.info("调用算法分析下发工具，算法类型: {}, 视频数量: {}",
				request.getAlgorithmType(), request.getVideoIds().size());

		// 获取视频信息
		List<VideoInfo> videos = VideoDataUtils.getVideosByIds(request.getVideoIds());

		// 提交异步任务
		Long taskId = taskManager.submitTaskWithResult(() -> {
			log.info("开始执行算法分析，算法类型: {}, 视频数量: {}",
					request.getAlgorithmType(), videos.size());

			try {
				// 模拟耗时操作
				Thread.sleep(5000);

				// 生成分析结果
				Map<String, Object> analysisResult = generateAnalysisResult(videos, request.getAlgorithmType());

				// 将结果存入结果缓存，并返回结果ID
				Long resultId = taskManager.createResultId(analysisResult);
				log.info("算法分析完成，结果已存入结果缓存，结果ID: {}", resultId);

				// todo 如果提供了chatId，通知任务完成
				return resultId;
			} catch (InterruptedException e) {
				log.error("算法分析任务被中断", e);
				throw new RuntimeException("算法分析任务被中断", e);
			}
		});

		// 构建响应
		return AlgorithmAnalysisResponse.builder()
				.taskId(taskId)
				.algorithmType(request.getAlgorithmType())
				.videoIds(request.getVideoIds())
				.status(TaskStatusEnum.PENDING)
				.estimatedCompletionTime(10)
				.chatId(request.getChatId())
				.nextTool("waitForAnalysisResult") // 指示需要等待结果
				.build();
	}

	/**
	 * 生成分析结果
	 */
	private Map<String, Object> generateAnalysisResult(List<VideoInfo> videos, String algorithmType) {
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> videoResults = new HashMap<>();

		for (VideoInfo video : videos) {
			Map<String, Object> videoResult = new HashMap<>();
			videoResult.put("videoId", video.getVideoId());
			videoResult.put("title", video.getTitle());

			// 根据算法类型生成不同的分析结果
			switch (algorithmType) {
				case "PERSON_DETECTION":
					videoResult.put("personCount", 3);
					videoResult.put("behaviors", List.of("走动", "站立", "交谈"));
					videoResult.put("anomalies", video.getTags().contains("人员入侵"));
					videoResult.put("conclusion", video.getTags().contains("人员入侵") ?
							"检测到未授权人员入侵，建议关注" : "正常人员活动，无异常");
					break;
				case "ANIMAL_DETECTION":
					videoResult.put("animalCount", 2);
					videoResult.put("animalTypes", List.of("狗", "猫"));
					videoResult.put("behaviors", List.of("奔跑", "休息"));
					videoResult.put("conclusion", video.getTags().contains("动物入侵") ?
							"检测到动物入侵，需要处理" : "检测到动物活动，情况正常");
					break;
				case "TRAFFIC_VIOLATION":
					videoResult.put("vehicleCount", 5);
					videoResult.put("violations", List.of("闯红灯", "逆行"));
					videoResult.put("severity", "中");
					videoResult.put("conclusion", "检测到交通违规行为，建议关注");
					break;
				default:
					videoResult.put("analysisType", algorithmType);
					videoResult.put("genericResult", "完成了通用分析");
					videoResult.put("conclusion", "完成了视频分析，请查看详细结果");
			}

			videoResults.put(video.getVideoId(), videoResult);
		}

		result.put("videoResults", videoResults);
		result.put("algorithmType", algorithmType);
		result.put("timestamp", System.currentTimeMillis());
		result.put("summary", String.format("使用%s算法分析了%d个视频", algorithmType, videos.size()));

		return result;
	}
}