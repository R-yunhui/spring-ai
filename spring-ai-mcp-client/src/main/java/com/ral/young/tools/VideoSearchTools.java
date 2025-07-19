package com.ral.young.tools;

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

/**
 * @author renyh
 * @description 视频检索服务
 * @date 2025/7/1 10:00
 * @since 1.0.0
 */
@Service
@Slf4j
@SuppressWarnings("preview")
public class VideoSearchTools {

	/**
	 * 用户问题关键字拆分工具
	 * 这是视频检索流程的第一步，用于从用户查询中提取关键词
	 *
	 * @param query 用户查询问题
	 * @return 拆分后的关键词列表
	 */
	@Tool(description = "第一步：从用户查询中提取关键词，用于视频检索。这是视频检索流程的起点，提取的关键词将用于后续的关键词检索。")
	public Map<String, Object> extractKeywords(
			@ToolParam(description = "用户的查询问题，例如'查找穿着黑色上衣白色牛仔裤的男人'") String query) {

		log.info("调用关键词拆分工具，查询问题: {}", query);

		// 模拟提取的关键词 - 针对人物外观查询
		List<Map<String, Object>> keywords = new ArrayList<>();
		keywords.add(Map.of("keyword", "黑色上衣", "weight", 0.95));
		keywords.add(Map.of("keyword", "白色牛仔裤", "weight", 0.95));
		keywords.add(Map.of("keyword", "男人", "weight", 0.90));
		keywords.add(Map.of("keyword", "人物", "weight", 0.85));

		Map<String, Object> result = new HashMap<>();
		result.put("keywords", keywords);
		result.put("mainTopic", "人物外观");
		result.put("intentType", "人物视频检索");
		result.put("visualAttributes", Map.of(
				"clothing", List.of("黑色上衣", "白色牛仔裤"),
				"gender", "男性",
				"age", "未指定"
		));

		return result;
	}

	/**
	 * 根据关键词检索视频工具
	 * 这是视频检索流程的第二步(A)，使用从extractKeywords获取的关键词进行检索
	 *
	 * @param keywords 关键词列表，来自extractKeywords的输出
	 * @param limit    返回结果数量上限
	 * @return 匹配的视频列表
	 */
	@Tool(description = "第二步(A)：使用关键词检索视频。依赖于extractKeywords工具的输出，使用提取的关键词列表进行视频检索。")
	public Map<String, Object> searchVideosByKeywords(
			@ToolParam(description = "关键词列表，应使用extractKeywords工具的输出中的keywords字段") List<String> keywords,
			@ToolParam(description = "返回结果数量上限，默认20") Integer limit) {

		log.info("调用关键词视频检索工具，关键词: {}, 限制数量: {}", keywords, limit);

		// 模拟检索结果 - 人物外观相关视频
		List<Map<String, Object>> videos = new ArrayList<>();
		videos.add(createPersonVideo("vid-001", "商场监控片段A",
				"商场一楼电梯附近，一名穿黑色上衣白色牛仔裤的男子正在看手机", 0.94));
		videos.add(createPersonVideo("vid-002", "街道监控记录B",
				"十字路口东南角，一名穿黑色T恤白色裤子的男子正在等待过马路", 0.86));
		videos.add(createPersonVideo("vid-003", "购物中心出入口",
				"购物中心北门，多名顾客进出，其中包括一名穿黑色上衣的男性", 0.72));

		Map<String, Object> result = new HashMap<>();
		result.put("videos", videos);
		result.put("totalMatches", videos.size());
		result.put("searchTime", "0.35s");

		log.info("关键词视频检索工具 检索结果数量: {}", videos.size());
		return result;
	}

	/**
	 * 根据用户问题embedding检索视频工具
	 * 这是视频检索流程的第二步(B)，使用用户查询的语义向量进行检索
	 *
	 * @param query 用户查询问题，原始查询文本
	 * @param limit 返回结果数量上限
	 * @return 匹配的视频列表
	 */
	@Tool(description = "第二步(B)：使用查询的语义向量检索视频。与关键词检索并行执行，直接使用用户原始查询进行语义向量检索。")
	public Map<String, Object> searchVideosByEmbedding(
			@ToolParam(description = "用户的完整查询，原始查询文本") String query,
			@ToolParam(description = "返回结果数量上限，默认20") Integer limit) {

		log.info("调用向量检索工具，查询: {}, 限制数量: {}", query, limit);

		// 模拟检索结果 - 人物外观相关视频
		List<Map<String, Object>> videos = new ArrayList<>();
		videos.add(createPersonVideo("vid-001", "商场监控片段A",
				"商场一楼电梯附近，一名穿黑色上衣白色牛仔裤的男子正在看手机", 0.95));
		videos.add(createPersonVideo("vid-004", "停车场监控C",
				"地下停车场B2层，一名身穿黑色夹克和浅色裤子的男性正在走向出口", 0.87));
		videos.add(createPersonVideo("vid-005", "咖啡厅内部视频",
				"咖啡厅靠窗座位，一名穿黑色上衣白色裤子的男顾客正在使用笔记本电脑", 0.82));

		Map<String, Object> result = new HashMap<>();
		result.put("videos", videos);
		result.put("totalMatches", videos.size());
		result.put("searchTime", "0.42s");

		log.info("embedding检索视频工具 检索结果数量: {}", videos.size());
		return result;
	}

	/**
	 * 大模型精确筛选视频工具
	 * 这是视频检索流程的第三步，对前两步检索结果进行精确筛选
	 *
	 * @param query          用户查询问题
	 * @param videoList      待筛选的视频列表，来自searchVideosByKeywords和searchVideosByEmbedding的合并结果
	 * @param filterCriteria 筛选条件
	 * @return 筛选后的视频列表
	 */
	@Tool(description = "第三步：对检索结果进行精确筛选。依赖于searchVideosByKeywords和searchVideosByEmbedding的输出，" +
			"将两者结果合并后进行语义理解筛选，得到最终的精确结果。")
	public Map<String, Object> filterVideoResults(
			@ToolParam(description = "用户的原始查询") String query,
			@ToolParam(description = "待筛选的视频列表，应合并searchVideosByKeywords和searchVideosByEmbedding的结果") List<Map<String, Object>> videoList,
			@ToolParam(description = "筛选条件，可选") Map<String, Object> filterCriteria) {

		log.info("调用视频精筛工具，查询: {}, 视频数量: {}", query, videoList.size());

		// 模拟筛选结果
		List<Map<String, Object>> filteredVideos = new ArrayList<>();
		List<Map<String, Object>> reasoning = new ArrayList<>();

		// 添加筛选后的视频
		filteredVideos.add(createPersonVideo("vid-001", "商场监控片段A",
				"商场一楼电梯附近，一名穿黑色上衣白色牛仔裤的男子正在看手机", 0.94));
		filteredVideos.add(createPersonVideo("vid-005", "咖啡厅内部视频",
				"咖啡厅靠窗座位，一名穿黑色上衣白色裤子的男顾客正在使用笔记本电脑", 0.82));

		// 筛选理由
		reasoning.add(Map.of(
				"videoId", "vid-001",
				"reason", "完全匹配用户查询条件：1)人物穿着黑色上衣；2)人物穿着白色牛仔裤；3)确认为成年男性"
		));
		reasoning.add(Map.of(
				"videoId", "vid-005",
				"reason", "高度匹配用户查询条件：1)人物穿着黑色上衣；2)人物穿着白色裤子(可能是牛仔裤)；3)确认为成年男性"
		));
		reasoning.add(Map.of(
				"videoId", "vid-002",
				"reason", "部分匹配但被过滤：虽然人物穿着黑色T恤和白色裤子，但视频质量不足以确认是否为牛仔裤"
		));
		reasoning.add(Map.of(
				"videoId", "vid-004",
				"reason", "部分匹配但被过滤：人物穿着黑色夹克，但裤子只能确认为浅色，不能确定是否为白色牛仔裤"
		));

		Map<String, Object> result = new HashMap<>();
		result.put("filteredVideos", filteredVideos);
		result.put("reasoning", reasoning);
		result.put("totalResults", filteredVideos.size());

		log.info("视频精筛工具 筛选结果数量: {}", filteredVideos.size());
		return result;
	}

	/**
	 * 视频检索报告生成工具
	 * 这是视频检索流程的最后一步，根据筛选结果生成报告，也可独立使用
	 *
	 * @param query          用户原始查询
	 * @param filteredVideos 筛选后的视频列表，可选参数
	 * @param reportType     报告类型，可选参数
	 * @return 生成的报告
	 */
	@Tool(description = "生成报告工具，支持两种主要场景：" +
			"1) 依赖检索结果：使用前面步骤检索和筛选的视频结果生成视频检索报告；" +
			"2) 独立生成报告：不依赖检索结果，直接根据用户查询和意图生成通用报告。")
	public Map<String, Object> generateSearchReport(
			@ToolParam(description = "用户的原始查询") String query,
			@ToolParam(description = "筛选后的视频列表，可选参数，如果不提供则生成通用报告") List<Map<String, Object>> filteredVideos,
			@ToolParam(description = "报告类型，仅在独立生成报告场景下使用") String reportType) {

		String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		log.info("调用报告生成工具，查询: {}, 视频数量: {}, 报告类型: {}, 当前时间: {}",
				query,
				filteredVideos != null ? filteredVideos.size() : 0,
				reportType,
				currentTime
		);

		// 判断报告类型
		if ((filteredVideos == null || filteredVideos.isEmpty()) &&
				(reportType != null && !reportType.equals("video_search"))) {
			// 生成通用报告（不依赖检索结果）
			return generateGenericReport(query, reportType);
		} else {
			// 生成视频检索报告
			return generateVideoSearchReport(query, filteredVideos);
		}
	}

	/**
	 * 确定报告标题
	 */
	private String determineReportTitle(String query, String reportType) {
		if (reportType == null || reportType.equals("video_search")) {
			return "视频检索报告";
		} else {
			return "主题分析报告";
		}
	}

	/**
	 * 生成视频检索报告
	 */
	private Map<String, Object> generateVideoSearchReport(String query, List<Map<String, Object>> filteredVideos) {
		// todo 需要调用LLM 生成通用报告

		Map<String, Object> report = new HashMap<>();
		report.put("type", "video_search");

		// 报告摘要
		Map<String, Object> summary = new HashMap<>();
		summary.put("totalResults", filteredVideos != null ? filteredVideos.size() : 0);
		summary.put("bestMatch", (filteredVideos == null || filteredVideos.isEmpty()) ?
				"未找到匹配视频" : STR."最佳匹配: \{filteredVideos.getFirst().get("title")}");
		report.put("summary", summary);

		// 结果列表
		if (filteredVideos != null && !filteredVideos.isEmpty()) {
			List<Map<String, Object>> results = new ArrayList<>();
			for (int i = 0; i < filteredVideos.size(); i++) {
				Map<String, Object> video = filteredVideos.get(i);
				Map<String, Object> result = new HashMap<>();
				result.put("rank", i + 1);
				result.put("title", video.get("title"));
				result.put("description", video.get("description"));
				result.put("relevanceScore", video.get("relevanceScore"));

				// 提取关键时间点（如果有）
				if (video.containsKey("keyFrames")) {
					List<Map<String, Object>> keyFrames = (List<Map<String, Object>>) video.get("keyFrames");
					List<String> timelines = new ArrayList<>();
					for (Map<String, Object> keyFrame : keyFrames) {
						timelines.add(STR."\{keyFrame.get("timestamp")}秒: \{keyFrame.get("description")}");
					}
					result.put("keyTime points", timelines);
				}
				results.add(result);
			}
			report.put("results", results);
		}

		// 简单建议
		List<String> recommendations = new ArrayList<>();
		if (filteredVideos != null && !filteredVideos.isEmpty()) {
			recommendations.add("建议优先查看排名靠前的视频，相关性更高");
		} else {
			recommendations.add("未找到匹配视频，建议调整搜索条件");
		}
		report.put("recommendations", recommendations);

		return report;
	}

	/**
	 * 生成通用报告（不依赖检索结果）
	 */
	private Map<String, Object> generateGenericReport(String query, String reportType) {
		// todo 需要调用LLM 生成通用报告

		Map<String, Object> report = new HashMap<>();
		report.put("type", reportType != null ? reportType : "general");

		// 通用报告结构
		report.put("sections", List.of(
				Map.of(
						"title", "分析概述",
						"content", "这是基于用户查询的分析概述"
				),
				Map.of(
						"title", "详细分析",
						"content", "这是详细分析内容"
				),
				Map.of(
						"title", "结论与建议",
						"content", "这是结论与建议内容"
				)
		));

		// 添加参考资料
		report.put("references", List.of(
				"参考资料1",
				"参考资料2"
		));

		return report;
	}

	/**
	 * 创建示例人物视频对象
	 */
	private Map<String, Object> createPersonVideo(String id, String title, String description, double relevance) {
		Map<String, Object> video = new HashMap<>();
		video.put("id", id);
		video.put("title", title);
		video.put("description", description);
		video.put("duration", 60 + (int) (Math.random() * 120));
		video.put("relevanceScore", relevance);
		video.put("timestamp", "2025-07-10T14:30:00Z");
		video.put("source", "监控摄像头");

		// 添加人物识别信息
		Map<String, Object> personDetection = new HashMap<>();
		personDetection.put("personCount", 1 + (int) (Math.random() * 5));
		personDetection.put("targetPerson", Map.of(
				"boundingBox", Map.of("x", 120, "y", 80, "width", 60, "height", 180),
				"confidence", 0.95,
				"attributes", Map.of(
						"gender", "male",
						"upperClothing", Map.of("color", "black", "type", "shirt", "confidence", 0.92),
						"lowerClothing", Map.of("color", "white", "type", "jeans", "confidence", 0.88),
						"accessories", List.of("none")
				)
		));
		video.put("personDetection", personDetection);

		// 添加关键帧信息
		List<Map<String, Object>> keyFrames = new ArrayList<>();
		keyFrames.add(Map.of(
				"timestamp", 15,
				"personVisible", true,
				"description", "目标人物进入画面"
		));
		keyFrames.add(Map.of(
				"timestamp", 45,
				"personVisible", true,
				"description", "目标人物最清晰角度"
		));
		video.put("keyFrames", keyFrames);

		return video;
	}
}