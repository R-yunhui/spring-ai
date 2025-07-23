package com.ral.young.tools;

import com.ral.young.dto.VideoInfo;
import com.ral.young.dto.request.AlgorithmMatchingRequest;
import com.ral.young.dto.response.AlgorithmMatchingResponse;
import com.ral.young.utils.VideoDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author renyh
 * @description 算法匹配工具
 * @date 2025/7/25 10:35
 * @since 1.0.0
 */
@Service
@Slf4j
public class AlgorithmMatchingTools {

	/**
	 * 算法匹配工具
	 * 根据用户问题自动判断是否可以匹配到平台内置算法
	 *
	 * @param query    用户的原始请求
	 * @param videoIds 视频id列表
	 * @return 算法匹配响应
	 */
	@Tool(description = "【视频分析必须步骤】在进行任何形式的视频分析前，必须先调用此工具判断是否有合适的内置算法。")
	public AlgorithmMatchingResponse matchAlgorithm(
			@ToolParam(description = "用户的原始查询全文，用于分析用户意图") String query,
			@ToolParam(description = "需要分析的视频ID列表") List<String> videoIds) {

		log.info("调用算法匹配工具，查询: {}, 视频数量: {}", query, videoIds.size());

		// 创建请求对象
		AlgorithmMatchingRequest request = AlgorithmMatchingRequest.builder()
				.query(query)
				.videoIds(videoIds)
				.build();

		String queryLower = query.toLowerCase();
		List<VideoInfo> videos = VideoDataUtils.getVideosByIds(videoIds);

		// 分析视频标签，确定可能的算法匹配
		Set<String> allTags = new HashSet<>();
		for (VideoInfo video : videos) {
			if (video.getTags() != null) {
				allTags.addAll(video.getTags());
			}
		}

		// 人员检测算法匹配
		if (containsAny(queryLower, Arrays.asList("人", "人员", "行人", "闯入", "入侵", "男", "女", "人物", "身份")) ||
				allTags.stream().anyMatch(tag -> tag.contains("人员") || tag.contains("男性") || tag.contains("女性"))) {
			return AlgorithmMatchingResponse.builder()
					.matched(true)
					.algorithmType("PERSON_DETECTION")
					.algorithmDescription("人员检测与行为分析算法")
					.videoIds(videoIds)
					.nextTool("dispatchAlgorithmAnalysis") // 直接指向算法下发工具
					.matchResult("成功匹配到人员检测与行为分析算法，可以分析视频中的人员活动和行为。")
					.build();
		}

		// 动物检测算法匹配
		if (containsAny(queryLower, Arrays.asList("动物", "狗", "猫", "鸟", "宠物")) ||
				allTags.stream().anyMatch(tag -> tag.contains("动物") || tag.contains("野狗") || tag.contains("野猫") || tag.contains("鸟类"))) {
			return AlgorithmMatchingResponse.builder()
					.matched(true)
					.algorithmType("ANIMAL_DETECTION")
					.algorithmDescription("动物检测与识别算法")
					.videoIds(videoIds)
					.nextTool("dispatchAlgorithmAnalysis") // 直接指向算法下发工具
					.matchResult("成功匹配到动物检测与识别算法，可以分析视频中的动物活动。")
					.build();
		}

		// 交通违规检测算法匹配
		if (containsAny(queryLower, Arrays.asList("交通", "车辆", "闯红灯", "逆行", "应急车道", "违规", "超速")) ||
				allTags.stream().anyMatch(tag -> tag.contains("交通") || tag.contains("车辆") || tag.contains("违规"))) {
			return AlgorithmMatchingResponse.builder()
					.matched(true)
					.algorithmType("TRAFFIC_VIOLATION")
					.algorithmDescription("交通违规检测算法")
					.videoIds(videoIds)
					.nextTool("dispatchAlgorithmAnalysis") // 直接指向算法下发工具
					.matchResult("成功匹配到交通违规检测算法，可以分析视频中的交通违规行为。")
					.build();
		}

		// 未匹配到算法，返回匹配失败信息，由提示词控制决策
		return AlgorithmMatchingResponse.builder()
				.matched(false)
				.videoIds(videoIds)
				.nextTool(null) // 不指定下一步工具，由提示词控制
				.matchResult("未能匹配到适合的内置算法，需要决策后续分析方式。")
				.build();
	}

	/**
	 * 判断文本是否包含任一关键词
	 */
	private boolean containsAny(String text, List<String> keywords) {
		for (String keyword : keywords) {
			if (text.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}