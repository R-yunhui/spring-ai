package com.ral.young.tools;

import com.ral.young.dto.VideoInfo;
import com.ral.young.dto.request.KeywordExtractRequest;
import com.ral.young.dto.request.VideoSearchRequest;
import com.ral.young.dto.response.KeywordExtractResponse;
import com.ral.young.dto.response.VideoSearchResponse;
import com.ral.young.utils.VideoDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author renyh
 * @description 视频检索相关工具
 * @date 2025/7/23 11:45
 * @since 1.0.0
 */
@Service
@Slf4j
public class VideoSearchTools {

	/**
	 * 用户问题关键字拆分工具
	 * 用于从用户查询中提取关键词
	 *
	 * @param request 关键词提取请求
	 * @return 关键词提取响应
	 */
	@Tool(description = "从用户查询中提取结构化的关键词，用于后续的视频检索。")
	public KeywordExtractResponse extractKeywords(KeywordExtractRequest request) {
		log.info("调用关键词拆分工具，查询问题: {}", request.getQuery());

		String query = request.getQuery();
		List<String> keywords = new ArrayList<>();

		// 根据查询内容提取关键词
		if (query.contains("黑色") || query.contains("上衣") || query.contains("男")) {
			keywords.addAll(Arrays.asList("黑色上衣", "男性", "人物"));
		} else if (query.contains("入侵") && query.contains("人")) {
			keywords.addAll(Arrays.asList("人员入侵", "安全事件", "陌生人"));
		} else if (query.contains("入侵") && (query.contains("动物") || query.contains("狗") || query.contains("猫") || query.contains("鸟"))) {
			keywords.addAll(Arrays.asList("动物入侵", "安全事件"));

			if (query.contains("狗")) keywords.add("野狗");
			if (query.contains("猫")) keywords.add("野猫");
			if (query.contains("鸟")) keywords.add("鸟类");
		} else if (query.contains("交通") || query.contains("闯红灯") || query.contains("逆行") || query.contains("应急车道")) {
			keywords.add("交通违规");

			if (query.contains("闯红灯")) keywords.add("闯红灯");
			if (query.contains("逆行")) keywords.add("逆行");
			if (query.contains("应急车道")) keywords.add("占用应急车道");
		} else {
			// 通用查询，将整个查询作为关键词
			keywords.add(query);
		}

		// 构建响应
		return KeywordExtractResponse.builder()
				.keywords(keywords)
				.originalQuery(query)
				.nextTool("searchVideos")
				.build();
	}

	/**
	 * 综合视频检索工具
	 * 整合关键词检索、向量检索和精确筛选功能
	 *
	 * @param request 视频搜索请求
	 * @return 视频搜索响应
	 */
	@Tool(description = "根据关键词或用户的原始查询进行视频检索。这是一个同步工具，会立即返回检索结果。")
	public VideoSearchResponse searchVideos(VideoSearchRequest request) {
		long startTime = System.currentTimeMillis();

		log.info("调用综合视频检索工具，查询: {}, 关键词: {}, 限制数量: {}, 使用向量检索: {}",
				request.getQuery(), request.getKeywords(), request.getLimit(), request.getUseEmbedding());

		// 默认值处理
		int resultLimit = (request.getLimit() != null) ? request.getLimit() : 10;
		boolean useEmbedding = (request.getUseEmbedding() != null) ? request.getUseEmbedding() : true;

		// 1. 关键词检索
		List<VideoInfo> keywordResults = performKeywordSearch(request.getKeywords());
		log.info("关键词检索结果数量: {}", keywordResults.size());

		// 2. 向量检索（如果启用）
		List<VideoInfo> embeddingResults = new ArrayList<>();
		if (useEmbedding) {
			embeddingResults = performEmbeddingSearch(request.getQuery());
			log.info("向量检索结果数量: {}", embeddingResults.size());
		}

		// 3. 合并结果
		List<VideoInfo> combinedResults = mergeSearchResults(keywordResults, embeddingResults);
		log.info("合并后结果数量: {}", combinedResults.size());

		// 4. 精确筛选
		List<VideoInfo> filteredVideos = filterResults(request.getQuery(), combinedResults);
		log.info("精确筛选后结果数量: {}", filteredVideos.size());

		// 5. 限制最终结果数量
		if (filteredVideos.size() > resultLimit) {
			filteredVideos = filteredVideos.subList(0, resultLimit);
		}

		// 提取视频ID列表
		List<String> videoIds = filteredVideos.stream()
				.map(VideoInfo::getVideoId)
				.collect(Collectors.toList());

		long endTime = System.currentTimeMillis();
		long searchTime = endTime - startTime;

		// 构建响应 - 修改nextTool为matchAlgorithm
		return VideoSearchResponse.builder()
				.videoIds(videoIds)
				.totalCount(videoIds.size())
				.nextTool(null) // 不固定指向下一步，由提示词根据用户需求决定
				.build();
	}

	/**
	 * 执行关键词检索
	 */
	private List<VideoInfo> performKeywordSearch(List<String> keywords) {
		log.info("执行关键词检索，关键词: {}", keywords);
		return VideoDataUtils.searchVideosByKeywords(keywords);
	}

	/**
	 * 执行向量检索
	 */
	private List<VideoInfo> performEmbeddingSearch(String query) {
		log.info("执行向量检索，查询: {}", query);

		// 模拟向量检索过程
		// 在实际实现中，这里应该调用向量检索服务
		// 目前使用模拟数据，根据查询内容返回相关视频

		List<VideoInfo> results = new ArrayList<>();

		// 根据查询内容匹配视频
		if (query.contains("黑色") || query.contains("上衣") || query.contains("男")) {
			// 查找与"黑色上衣男性"相关的视频
			results.addAll(VideoDataUtils.searchVideosByTags(Arrays.asList("黑色上衣", "男性")));
		} else if (query.contains("入侵") && query.contains("人")) {
			// 查找与"人员入侵"相关的视频
			results.addAll(VideoDataUtils.searchVideosByTags(List.of("人员入侵")));
		} else if (query.contains("入侵") && (query.contains("动物") || query.contains("狗") || query.contains("猫") || query.contains("鸟"))) {
			// 查找与"动物入侵"相关的视频
			results.addAll(VideoDataUtils.searchVideosByTags(List.of("动物入侵")));
		} else if (query.contains("交通") || query.contains("闯红灯") || query.contains("逆行") || query.contains("应急车道")) {
			// 查找与"交通违规"相关的视频
			results.addAll(VideoDataUtils.searchVideosByTags(List.of("交通违规")));
		} else {
			// 通用查询，返回所有视频
			results.addAll(VideoDataUtils.getAllVideos());
		}

		// 模拟向量相似度排序
		// 在实际实现中，这里应该根据向量相似度排序
		// 目前简单随机打乱结果列表，模拟不同的排序结果
		Collections.shuffle(results);

		return results;
	}

	/**
	 * 合并检索结果
	 */
	private List<VideoInfo> mergeSearchResults(List<VideoInfo> keywordResults, List<VideoInfo> embeddingResults) {
		// 使用Set避免重复
		Set<String> addedIds = new HashSet<>();
		List<VideoInfo> mergedResults = new ArrayList<>();

		// 添加关键词检索结果
		for (VideoInfo video : keywordResults) {
			if (!addedIds.contains(video.getVideoId())) {
				mergedResults.add(video);
				addedIds.add(video.getVideoId());
			}
		}

		// 添加向量检索结果
		for (VideoInfo video : embeddingResults) {
			if (!addedIds.contains(video.getVideoId())) {
				mergedResults.add(video);
				addedIds.add(video.getVideoId());
			}
		}

		return mergedResults;
	}

	/**
	 * 精确筛选结果
	 */
	private List<VideoInfo> filterResults(String query, List<VideoInfo> videos) {
		// 模拟LLM精确筛选过程
		// 在实际实现中，这里应该调用LLM服务进行精确筛选
		// 目前使用简单的规则筛选

		List<VideoInfo> filteredVideos = new ArrayList<>();

		for (VideoInfo video : videos) {
			// 根据查询内容和视频标签进行筛选
			List<String> tags = video.getTags();
			String description = video.getDescription().toLowerCase();
			String title = video.getTitle().toLowerCase();
			String queryLower = query.toLowerCase();

			boolean isRelevant = title.contains(queryLower) || description.contains(queryLower);

			// 检查标题和描述是否包含查询关键词

			// 检查标签是否与查询相关
			if (!isRelevant && tags != null) {
				for (String tag : tags) {
					if (queryLower.contains(tag.toLowerCase()) || tag.toLowerCase().contains(queryLower)) {
						isRelevant = true;
						break;
					}
				}
			}

			// 特定类型的查询处理
			if (!isRelevant) {
				if ((queryLower.contains("黑色") || queryLower.contains("上衣") || queryLower.contains("男")) &&
						(tags.contains("黑色上衣") || tags.contains("男性"))) {
					isRelevant = true;
				} else if (queryLower.contains("入侵") && queryLower.contains("人") && tags.contains("人员入侵")) {
					isRelevant = true;
				} else if (queryLower.contains("入侵") &&
						(queryLower.contains("动物") || queryLower.contains("狗") || queryLower.contains("猫") || queryLower.contains("鸟")) &&
						tags.contains("动物入侵")) {
					isRelevant = true;
				} else if ((queryLower.contains("交通") || queryLower.contains("闯红灯") || queryLower.contains("逆行") || queryLower.contains("应急车道")) &&
						tags.contains("交通违规")) {
					isRelevant = true;
				}
			}

			if (isRelevant) {
				filteredVideos.add(video);
			}
		}

		return filteredVideos;
	}
}