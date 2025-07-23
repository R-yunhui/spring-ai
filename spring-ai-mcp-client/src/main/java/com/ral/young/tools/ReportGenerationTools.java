package com.ral.young.tools;

import com.ral.young.dto.VideoInfo;
import com.ral.young.dto.request.ReportRequest;
import com.ral.young.dto.response.ReportResponse;
import com.ral.young.manager.TaskManager;
import com.ral.young.service.VideoSearchService;
import com.ral.young.utils.VideoDataUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author renyh
 * @description 报告生成相关工具
 * @date 2025/7/23 12:30
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("preview")
public class ReportGenerationTools {

	@Resource
	private TaskManager taskManager;

	@Resource
	@Lazy
	private VideoSearchService videoSearchService;

	/**
	 * 综合报告生成工具
	 * 支持检索报告、分析报告和综合报告
	 *
	 * @return 生成的报告
	 */
	@Tool(description = "生成各类报告，既可以基于视频分析结果生成报告，也可以直接生成与视频无关的主题报告。", returnDirect = true)
	public ReportResponse generateReport(ReportRequest request) {
		log.info("调用报告生成工具，报告类型: {}", request.getReportType());

		String reportContent;

		switch (request.getReportType()) {
			case "search":
				reportContent = generateSearchReport(request.getVideoIdList());
				break;
			case "analysis":
				if (request.getTaskId() != null) {
					// 从任务结果中获取分析结果
					Object taskResult = taskManager.getTaskResult(request.getTaskId());
					if (taskResult instanceof Long) {
						// 如果任务结果是结果ID，从结果缓存中获取结果
						Object analysisResult = taskManager.getResult((Long) taskResult);
						reportContent = generateAnalysisReport(analysisResult);
					} else {
						reportContent = "无法获取分析结果";
					}
				} else if (request.getResultId() != null) {
					// 直接从结果缓存中获取结果
					Object analysisResult = taskManager.getResult(request.getResultId());
					reportContent = generateAnalysisReport(analysisResult);
				} else {
					reportContent = "缺少分析结果";
				}
				break;
			case "comprehensive":
				reportContent = generateComprehensiveReport(request.getQuery());
				break;
			default:
				reportContent = STR."不支持的报告类型: \{request.getReportType()}";
		}

		// 生成报告ID
		String reportId = UUID.randomUUID().toString();

		// 构建响应
		return ReportResponse.builder()
				.reportId(reportId)
				.content(reportContent)
				.format("markdown")
				.build();
	}

	/**
	 * 生成检索报告
	 */
	private String generateSearchReport(List<String> videoIds) {
		if (videoIds == null || videoIds.isEmpty()) {
			return "# 检索报告\n\n未提供视频ID";
		}

		List<VideoInfo> videos = VideoDataUtils.getVideosByIds(videoIds);

		StringBuilder report = new StringBuilder();
		report.append("# 视频检索报告\n\n");
		report.append("## 摘要\n\n");
		report.append("本次检索共找到 ").append(videos.size()).append(" 个相关视频。\n\n");

		report.append("## 检索结果详情\n\n");

		for (int i = 0; i < videos.size(); i++) {
			VideoInfo video = videos.get(i);
			report.append("### ").append(i + 1).append(". ").append(video.getTitle()).append("\n\n");
			report.append("- **视频ID**: ").append(video.getVideoId()).append("\n");
			report.append("- **描述**: ").append(video.getDescription()).append("\n");
			report.append("- **时间段**: ").append(video.getStartTime()).append(" 至 ").append(video.getEndTime()).append("\n");
			report.append("- **时长**: ").append(video.getDuration()).append(" 秒\n");
			report.append("- **标签**: ").append(String.join(", ", video.getTags())).append("\n\n");
		}

		report.append("## 结论\n\n");
		report.append("根据检索结果，");

		// 根据视频标签生成结论
		boolean hasSecurityEvent = false;
		boolean hasPersonDetection = false;
		boolean hasTrafficViolation = false;

		for (VideoInfo video : videos) {
			List<String> tags = video.getTags();
			if (tags.contains("人员入侵") || tags.contains("动物入侵")) {
				hasSecurityEvent = true;
			}
			if (tags.contains("黑色上衣") && tags.contains("男性")) {
				hasPersonDetection = true;
			}
			if (tags.contains("交通违规")) {
				hasTrafficViolation = true;
			}
		}

		if (hasSecurityEvent) {
			report.append("发现安全事件，建议关注相关视频并采取适当措施。");
		} else if (hasPersonDetection) {
			report.append("发现符合目标人物特征的视频，可进一步分析确认身份。");
		} else if (hasTrafficViolation) {
			report.append("发现交通违规行为，可进一步分析违规类型和严重程度。");
		} else {
			report.append("未发现明显异常，建议根据需要进行进一步分析。");
		}

		report.append("\n\n");
		report.append("报告生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		return report.toString();
	}

	/**
	 * 生成分析报告
	 */
	private String generateAnalysisReport(Object analysisResultObj) {
		if (analysisResultObj == null) {
			return "# 分析报告\n\n无法获取分析结果";
		}

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> analysisResult = (Map<String, Object>) analysisResultObj;

			StringBuilder report = new StringBuilder();
			report.append("# 视频分析报告\n\n");

			// 添加摘要
			report.append("## 摘要\n\n");
			String summary = (String) analysisResult.get("summary");
			if (summary != null) {
				report.append(summary).append("\n\n");
			} else {
				report.append("本次分析未生成摘要。\n\n");
			}

			// 添加分析类型
			String analysisType = (String) analysisResult.get("analysisType");
			if (analysisType != null) {
				report.append("分析类型: ").append(analysisType).append("\n\n");
			}

			// 添加分析结果详情
			report.append("## 分析结果详情\n\n");

			@SuppressWarnings("unchecked")
			Map<String, Object> videoResults = (Map<String, Object>) analysisResult.get("videoResults");
			if (videoResults != null && !videoResults.isEmpty()) {
				int index = 1;
				for (Map.Entry<String, Object> entry : videoResults.entrySet()) {
					String videoId = entry.getKey();
					@SuppressWarnings("unchecked")
					Map<String, Object> videoResult = (Map<String, Object>) entry.getValue();

					// 获取视频信息
					VideoInfo video = VideoDataUtils.getVideoById(videoId);
					String title = video != null ? video.getTitle() : (String) videoResult.get("title");

					report.append("### ").append(index++).append(". ").append(title).append("\n\n");
					report.append("- **视频ID**: ").append(videoId).append("\n");

					// 添加事件信息
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> events = (List<Map<String, Object>>) videoResult.get("events");
					if (events != null && !events.isEmpty()) {
						report.append("- **检测到的事件**: ").append(events.size()).append(" 个\n\n");
						report.append("| 事件类型 | 置信度 | 时间点 | 描述 | 严重程度 |\n");
						report.append("|---------|--------|--------|------|----------|\n");

						for (Map<String, Object> event : events) {
							report.append("| ")
									.append(event.get("eventType")).append(" | ")
									.append(event.get("confidence")).append(" | ")
									.append(event.get("timestamp")).append("秒 | ")
									.append(event.get("description")).append(" | ")
									.append(event.get("severity")).append(" |\n");
						}
						report.append("\n");
					} else {
						report.append("- **检测到的事件**: 无\n\n");
					}

					// 添加结论
					String conclusion = (String) videoResult.get("conclusion");
					if (conclusion != null) {
						report.append("- **结论**: ").append(conclusion).append("\n\n");
					}
				}
			} else {
				report.append("未获取到分析结果详情。\n\n");
			}

			// 添加总体结论
			report.append("## 总体结论\n\n");
			if (summary != null) {
				String[] parts = summary.split("。");
				if (parts.length > 1) {
					report.append(parts[parts.length - 1]).append("。\n\n");
				} else {
					report.append(summary).append("\n\n");
				}
			} else {
				report.append("无法生成总体结论。\n\n");
			}

			// 添加生成时间
			report.append("报告生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

			return report.toString();
		} catch (Exception e) {
			log.error("生成分析报告失败", e);
			return "# 分析报告\n\n生成报告时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 生成通用报告（不依赖检索结果或分析结果）
	 */
	private String generateComprehensiveReport(String query) {
		// 构建系统提示词
		String systemPrompt = """
				你是一个专业的报告生成助手。请根据用户的查询，生成一份格式规范的Markdown格式报告。
				报告应当客观、专业、简洁，根据用户查询的主题和意图进行深入分析。

				报告应包含以下部分：
				1. 报告标题：简明扼要地概括主题
				2. 摘要：对主题的简要概述
				3. 主要内容：分析用户查询的关键点，提供相关信息和见解
				4. 结论与建议：基于分析提供的结论和建议
				5. 参考资料：如有必要，列出相关参考资料

				请使用Markdown语法格式化报告，包括标题(#)、列表(-)、表格等元素，确保报告结构清晰、易读。
				不要添加任何额外的解释或前后文，直接返回Markdown格式的报告内容。
				""";

		// 构建用户提示词
		String userPrompt = String.format("""
				请根据以下查询生成一份专业的报告：

				查询: %s
				报告类型: %s

				请分析查询意图，提供相关的深入见解，并生成一份结构完整的Markdown格式报告。
				""", query, "comprehensive");

		// 调用大模型生成报告
		SystemMessage systemMessage = new SystemMessage(systemPrompt);
		UserMessage userMessage = new UserMessage(userPrompt);
		return videoSearchService.getMarkdownReport(systemMessage, userMessage);
	}

}