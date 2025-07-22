package com.ral.young.tools;

import cn.hutool.json.JSONUtil;
import com.ral.young.utils.VideoDataUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author renyh
 * @description 视频检索相关工具
 * @date 2025/7/1 10:00
 * @since 1.0.0
 */
@Service
@Slf4j
public class VideoSearchTools {

	/**
	 * 用户问题关键字拆分工具
	 * 用于从用户查询中提取关键词
	 *
	 * @param query 用户查询问题
	 * @return 拆分后的关键词列表
	 */
	@Tool(description = "从用户查询中提取关键词，用于视频检索。这是视频检索流程的第一步，提取的关键词将用于后续的视频检索。")
	public Map<String, Object> extractKeywords(
			@ToolParam(description = "用户的查询问题，例如'查找穿着黑色上衣白色牛仔裤的男人'") String query) {

		log.info("调用关键词拆分工具，查询问题: {}", query);

		// 模拟提取的关键词
		List<Map<String, Object>> keywords = new ArrayList<>();

		if (query.contains("黑色") || query.contains("上衣") || query.contains("牛仔裤") || query.contains("男")) {
			// 人物外观查询
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
		} else if (query.contains("入侵") || query.contains("异常") || query.contains("事件")) {
			// 安全事件查询
			keywords.add(Map.of("keyword", "入侵", "weight", 0.95));
			keywords.add(Map.of("keyword", "异常行为", "weight", 0.90));
			keywords.add(Map.of("keyword", "安全事件", "weight", 0.85));

			Map<String, Object> result = new HashMap<>();
			result.put("keywords", keywords);
			result.put("mainTopic", "安全事件");
			result.put("intentType", "事件检测");
			result.put("eventAttributes", Map.of(
					"eventType", List.of("入侵", "异常行为"),
					"severity", "高",
					"timeRange", "全天"
			));

			return result;
		} else if (query.contains("烟") || query.contains("火") || query.contains("烟雾")) {
			// 火灾安全查询
			keywords.add(Map.of("keyword", "烟雾", "weight", 0.95));
			keywords.add(Map.of("keyword", "火灾", "weight", 0.90));
			keywords.add(Map.of("keyword", "安全隐患", "weight", 0.85));

			Map<String, Object> result = new HashMap<>();
			result.put("keywords", keywords);
			result.put("mainTopic", "火灾安全");
			result.put("intentType", "事件检测");
			result.put("eventAttributes", Map.of(
					"eventType", List.of("烟雾", "火灾"),
					"severity", "高",
					"timeRange", "全天"
			));

			return result;
		} else {
			// 通用查询
			keywords.add(Map.of("keyword", "视频", "weight", 0.80));
			keywords.add(Map.of("keyword", query, "weight", 0.95));

			Map<String, Object> result = new HashMap<>();
			result.put("keywords", keywords);
			result.put("mainTopic", "通用查询");
			result.put("intentType", "视频检索");

			return result;
		}
	}

	/**
	 * 综合视频检索工具
	 * 整合关键词检索、向量检索和精确筛选功能
	 *
	 * @param query        用户原始查询
	 * @param keywords     关键词列表，来自extractKeywords的输出
	 * @param limit        返回结果数量上限
	 * @param useEmbedding 是否使用向量检索，默认true
	 * @return 筛选后的视频列表
	 */
	@Tool(description = "综合视频检索工具，整合了关键词检索、向量检索和精确筛选功能。提供关键词后，工具会自动执行检索和筛选，返回最终结果。")
	public Map<String, Object> searchVideos(
			@ToolParam(description = "用户的原始查询") String query,
			@ToolParam(description = "关键词列表，应使用extractKeywords工具的输出中的keywords字段") List<Map<String, Object>> keywords,
			@ToolParam(description = "返回结果数量上限，默认10") Integer limit,
			@ToolParam(description = "是否使用向量检索，默认true") Boolean useEmbedding) {

		log.info("调用综合视频检索工具，查询: {}, 关键词数量: {}, 限制数量: {}, 使用向量检索: {}",
				query, keywords.size(), limit, useEmbedding);

		// 默认值处理
		int resultLimit = (limit != null) ? limit : 10;
		boolean useEmbeddingSearch = (useEmbedding != null) ? useEmbedding : true;

		// 1. 关键词检索
		List<Map<String, Object>> keywordResults = performKeywordSearch(keywords, resultLimit);
		log.info("关键词检索结果数量: {}", keywordResults.size());

		// 2. 向量检索（如果启用）
		List<Map<String, Object>> embeddingResults = new ArrayList<>();
		if (useEmbeddingSearch) {
			embeddingResults = performEmbeddingSearch(query, resultLimit);
			log.info("向量检索结果数量: {}", embeddingResults.size());
		}

		// 3. 合并结果
		List<Map<String, Object>> combinedResults = mergeSearchResults(keywordResults, embeddingResults);
		log.info("合并后结果数量: {}", combinedResults.size());

		// 4. 精确筛选
		List<Map<String, Object>> filteredVideos = filterResults(query, combinedResults);
		List<Map<String, Object>> reasoning = generateReasoning(query, filteredVideos);

		// 5. 限制最终结果数量
		if (filteredVideos.size() > resultLimit) {
			filteredVideos = filteredVideos.subList(0, resultLimit);
			reasoning = reasoning.subList(0, resultLimit);
		}

		// 构建返回结果
		Map<String, Object> result = new HashMap<>();
		result.put("filteredVideos", filteredVideos);
		result.put("reasoning", reasoning);
		result.put("totalResults", filteredVideos.size());
		result.put("searchMethods", useEmbeddingSearch ?
				List.of("keywords", "embedding") : List.of("keywords"));
		result.put("searchTime", "0.68s");

		log.info("综合视频检索工具 最终结果数量: {}, 详细检测结果数据: {}", filteredVideos.size(), JSONUtil.toJsonPrettyStr(filteredVideos));
		return result;
	}

	/**
	 * 执行关键词检索
	 */
	private List<Map<String, Object>> performKeywordSearch(List<Map<String, Object>> keywords, int limit) {
		// 提取关键词字符串
		List<String> keywordStrings = new ArrayList<>();
		for (Map<String, Object> keyword : keywords) {
			keywordStrings.add((String) keyword.get("keyword"));
		}

		String keywordsStr = String.join(" ", keywordStrings);
		List<Map<String, Object>> videos = new ArrayList<>();

		if (keywordsStr.contains("黑色") || keywordsStr.contains("上衣") || keywordsStr.contains("男")) {
			// 人物外观相关视频
			videos.add(VideoDataUtils.createPersonVideo("vid_001", "商场监控片段A",
					"商场一楼电梯附近，一名穿黑色上衣白色牛仔裤的男子正在看手机", 0.94));
			videos.add(VideoDataUtils.createPersonVideo("vid_002", "街道监控记录B",
					"十字路口东南角，一名穿黑色T恤白色裤子的男子正在等待过马路", 0.86));
			videos.add(VideoDataUtils.createPersonVideo("vid_003", "购物中心出入口",
					"购物中心北门，多名顾客进出，其中包括一名穿黑色上衣的男性", 0.72));
		} else if (keywordsStr.contains("入侵") || keywordsStr.contains("异常") || keywordsStr.contains("事件")) {
			// 安全事件相关视频
			videos.add(VideoDataUtils.createSecurityVideo("vid_101", "仓库后门监控A",
					"仓库后门区域，一名陌生人尝试撬门进入", 0.96));
			videos.add(VideoDataUtils.createSecurityVideo("vid_102", "办公区走廊监控B",
					"办公区走廊，非工作时间有人员活动", 0.88));
			videos.add(VideoDataUtils.createSecurityVideo("vid_103", "停车场监控C",
					"地下停车场，有人在车辆间徘徊", 0.79));
		} else if (keywordsStr.contains("烟") || keywordsStr.contains("火") || keywordsStr.contains("烟雾")) {
			// 火灾安全相关视频
			videos.add(VideoDataUtils.createFireSafetyVideo("vid_201", "厨房监控A",
					"厨房区域，炉灶上出现明显烟雾", 0.97));
			videos.add(VideoDataUtils.createFireSafetyVideo("vid_202", "走廊监控B",
					"三楼走廊，烟雾探测器被触发", 0.89));
			videos.add(VideoDataUtils.createFireSafetyVideo("vid_203", "仓储区监控C",
					"仓储区角落，有微弱烟雾出现", 0.75));
		} else {
			// 通用视频
			videos.add(VideoDataUtils.createGenericVideo("vid_301", "办公区全景",
					"办公区日常活动画面", 0.70));
			videos.add(VideoDataUtils.createGenericVideo("vid_302", "前台接待区",
					"公司前台接待区域的监控画面", 0.65));
		}

		return videos;
	}

	/**
	 * 执行向量检索
	 */
	private List<Map<String, Object>> performEmbeddingSearch(String query, int limit) {
		List<Map<String, Object>> videos = new ArrayList<>();

		if (query.contains("黑色") || query.contains("上衣") || query.contains("男")) {
			// 人物外观相关视频
			videos.add(VideoDataUtils.createPersonVideo("vid_001", "商场监控片段A",
					"商场一楼电梯附近，一名穿黑色上衣白色牛仔裤的男子正在看手机", 0.95));
			videos.add(VideoDataUtils.createPersonVideo("vid_004", "停车场监控C",
					"地下停车场B2层，一名身穿黑色夹克和浅色裤子的男性正在走向出口", 0.87));
			videos.add(VideoDataUtils.createPersonVideo("vid_005", "咖啡厅内部视频",
					"咖啡厅靠窗座位，一名穿黑色上衣白色裤子的男顾客正在使用笔记本电脑", 0.82));
		} else if (query.contains("入侵") || query.contains("异常") || query.contains("事件")) {
			// 安全事件相关视频
			videos.add(VideoDataUtils.createSecurityVideo("vid_101", "仓库后门监控A",
					"仓库后门区域，一名陌生人尝试撬门进入", 0.98));
			videos.add(VideoDataUtils.createSecurityVideo("vid_104", "围墙监控D",
					"公司围墙外，有人徘徊并尝试攀爬", 0.91));
			videos.add(VideoDataUtils.createSecurityVideo("vid_105", "服务器室入口",
					"服务器室入口，有未授权人员尝试进入", 0.85));
		} else if (query.contains("烟") || query.contains("火") || query.contains("烟雾")) {
			// 火灾安全相关视频
			videos.add(VideoDataUtils.createFireSafetyVideo("vid_201", "厨房监控A",
					"厨房区域，炉灶上出现明显烟雾", 0.99));
			videos.add(VideoDataUtils.createFireSafetyVideo("vid_204", "电气室监控D",
					"电气室内，配电箱附近出现异常烟雾", 0.93));
			videos.add(VideoDataUtils.createFireSafetyVideo("vid_205", "实验室监控",
					"实验室角落，有化学物质反应产生的烟雾", 0.88));
		} else {
			// 通用视频
			videos.add(VideoDataUtils.createGenericVideo("vid_301", "办公区全景",
					"办公区日常活动画面", 0.75));
			videos.add(VideoDataUtils.createGenericVideo("vid_302", "前台接待区",
					"公司前台接待区域的监控画面", 0.70));
			videos.add(VideoDataUtils.createGenericVideo("vid_303", "会议室监控",
					"主会议室的监控画面，显示会议进行中", 0.68));
		}

		return videos;
	}

	/**
	 * 合并检索结果
	 */
	private List<Map<String, Object>> mergeSearchResults(List<Map<String, Object>> keywordResults,
														 List<Map<String, Object>> embeddingResults) {
		// 使用Set避免重复
		Set<String> addedIds = new HashSet<>();
		List<Map<String, Object>> mergedResults = new ArrayList<>();

		// 添加关键词检索结果
		for (Map<String, Object> video : keywordResults) {
			String id = (String) video.get("id");
			if (!addedIds.contains(id)) {
				mergedResults.add(video);
				addedIds.add(id);
			}
		}

		// 添加向量检索结果
		for (Map<String, Object> video : embeddingResults) {
			String id = (String) video.get("id");
			if (!addedIds.contains(id)) {
				mergedResults.add(video);
				addedIds.add(id);
			}
		}

		return mergedResults;
	}

	/**
	 * 精确筛选结果
	 */
	private List<Map<String, Object>> filterResults(String query, List<Map<String, Object>> videoList) {
		List<Map<String, Object>> filteredVideos = new ArrayList<>();

		// 根据查询内容筛选不同类型的视频
		if (query.contains("黑色") || query.contains("上衣") || query.contains("男")) {
			// 筛选人物外观相关视频
			for (Map<String, Object> video : videoList) {
				String description = (String) video.get("description");
				if (description.contains("黑色") && description.contains("男")) {
					filteredVideos.add(video);
				}
			}
		} else if (query.contains("入侵") || query.contains("异常") || query.contains("事件")) {
			// 筛选安全事件相关视频
			for (Map<String, Object> video : videoList) {
				String description = (String) video.get("description");
				if (description.contains("尝试") || description.contains("陌生人") || description.contains("未授权")) {
					filteredVideos.add(video);
				}
			}
		} else if (query.contains("烟") || query.contains("火") || query.contains("烟雾")) {
			// 筛选火灾安全相关视频
			for (Map<String, Object> video : videoList) {
				String description = (String) video.get("description");
				if (description.contains("烟雾") || description.contains("火")) {
					filteredVideos.add(video);
				}
			}
		} else {
			// 通用筛选，按相关性排序
			videoList.sort((v1, v2) -> {
				double score1 = (double) v1.get("relevanceScore");
				double score2 = (double) v2.get("relevanceScore");
				return Double.compare(score2, score1);  // 降序排列
			});

			filteredVideos = new ArrayList<>(videoList);
		}

		return filteredVideos;
	}

	/**
	 * 生成筛选理由
	 */
	private List<Map<String, Object>> generateReasoning(String query, List<Map<String, Object>> filteredVideos) {
		List<Map<String, Object>> reasoning = new ArrayList<>();

		for (Map<String, Object> video : filteredVideos) {
			String id = (String) video.get("id");
			String description = (String) video.get("description");

			if (query.contains("黑色") || query.contains("上衣") || query.contains("男")) {
				reasoning.add(Map.of(
						"videoId", id,
						"reason", "匹配用户查询条件：包含黑色服装和男性人物"
				));
			} else if (query.contains("入侵") || query.contains("异常") || query.contains("事件")) {
				reasoning.add(Map.of(
						"videoId", id,
						"reason", "匹配用户查询条件：包含潜在入侵或异常行为"
				));
			} else if (query.contains("烟") || query.contains("火") || query.contains("烟雾")) {
				reasoning.add(Map.of(
						"videoId", id,
						"reason", "匹配用户查询条件：包含烟雾或火灾相关情况"
				));
			} else {
				reasoning.add(Map.of(
						"videoId", id,
						"reason", "基于相关性评分筛选的高匹配度结果"
				));
			}
		}

		return reasoning;
	}
}