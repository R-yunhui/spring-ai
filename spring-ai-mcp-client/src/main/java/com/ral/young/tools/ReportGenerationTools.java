package com.ral.young.tools;

import com.ral.young.service.VideoSearchService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @author renyh
 * @description 报告生成相关工具
 * @date 2025/7/1 10:00
 * @since 1.0.0
 */
@Service
@Slf4j
public class ReportGenerationTools {

    @Resource
    @Lazy
    private VideoSearchService videoSearchService;

    /**
     * 综合报告生成工具
     * 支持检索报告、分析报告和综合报告
     *
     * @param query           用户原始查询
     * @param filteredVideos  筛选后的视频列表，可选参数
     * @param analysisResults 视频分析结果，可选参数
     * @param reportType      报告类型，可选参数
     * @return 生成的报告
     */
    @Tool(description = "多功能报告生成工具，支持三种主要场景：" +
            "1) 检索报告：基于视频检索结果生成报告，需提供filteredVideos参数；" +
            "2) 分析报告：基于视频分析结果生成报告，需提供analysisResults参数；" +
            "3) 独立报告：不依赖检索或分析结果，直接根据用户查询生成主题报告，需提供reportType参数。",
            returnDirect = true)
    public Map<String, Object> generateReport(
            @ToolParam(description = "用户的原始查询") String query,
            @ToolParam(description = "筛选后的视频列表，用于生成检索报告，可选") List<Map<String, Object>> filteredVideos,
            @ToolParam(description = "视频分析结果，用于生成分析报告，可选") List<Map<String, Object>> analysisResults,
            @ToolParam(description = "报告类型，仅在独立生成报告场景下使用，可选") String reportType) {

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        log.info("调用报告生成工具，查询: {}, 视频数量: {}, 分析结果数量: {}, 报告类型: {}, 当前时间: {}",
                query,
                filteredVideos != null ? filteredVideos.size() : 0,
                analysisResults != null ? analysisResults.size() : 0,
                reportType,
                currentTime
        );

        // 判断报告类型
        if (filteredVideos != null && !filteredVideos.isEmpty() && analysisResults != null && !analysisResults.isEmpty()) {
            // 综合报告：同时包含检索结果和分析结果
            return generateComprehensiveReport(query, filteredVideos, analysisResults);
        } else if (filteredVideos != null && !filteredVideos.isEmpty()) {
            // 检索报告：仅包含检索结果
            return generateSearchReport(query, filteredVideos);
        } else if (analysisResults != null && !analysisResults.isEmpty()) {
            // 分析报告：仅包含分析结果
            return generateAnalysisReport(query, analysisResults);
        } else {
            // 独立报告：不依赖检索或分析结果
            return generateGenericReport(query, reportType);
        }
    }

    /**
     * 生成检索报告
     */
    private Map<String, Object> generateSearchReport(String query, List<Map<String, Object>> filteredVideos) {
        // 构建系统提示词
        String systemPrompt = """
                你是一个专业的视频检索报告生成助手。请根据提供的视频检索结果，生成一份格式规范的Markdown格式报告。
                报告应当客观、专业、简洁，包含以下部分：

                1. 报告标题和摘要：包括检索条件、结果数量和最佳匹配
                2. 检索结果详情：按相关性排序的视频列表，包含标题、描述和相关性评分
                3. 关键时间点：如果有关键帧信息，请提取并展示
                4. 分析与建议：基于检索结果提供简要分析和建议

                请使用Markdown语法格式化报告，包括标题(#)、列表(-)、表格等元素，确保报告结构清晰、易读。
                不要添加任何额外的解释或前后文，直接返回Markdown格式的报告内容。
                """;

        // 构建用户提示词
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("请根据以下信息生成视频检索报告：\n\n");
        userPromptBuilder.append("用户查询: ").append(query).append("\n\n");
        userPromptBuilder.append("检索结果数量: ").append(filteredVideos.size()).append("\n\n");

        // 添加视频信息
        userPromptBuilder.append("检索到的视频:\n");
        for (int i = 0; i < filteredVideos.size(); i++) {
            Map<String, Object> video = filteredVideos.get(i);
            userPromptBuilder.append(i + 1).append(". 标题: ").append(video.get("title")).append("\n");
            userPromptBuilder.append("   描述: ").append(video.get("description")).append("\n");
            userPromptBuilder.append("   相关性评分: ").append(video.get("relevanceScore")).append("\n");
            userPromptBuilder.append("   视频ID: ").append(video.get("id")).append("\n");
            userPromptBuilder.append("   时长: ").append(video.get("duration")).append("秒\n");
            userPromptBuilder.append("   来源: ").append(video.get("source")).append("\n");

            // 添加视频特定属性（根据视频类型）
            if (video.containsKey("personDetection")) {
                addPersonDetectionInfo(userPromptBuilder, video);
            } else if (video.containsKey("securityEvents")) {
                addSecurityEventInfo(userPromptBuilder, video);
            } else if (video.containsKey("fireSafetyInfo")) {
                addFireSafetyInfo(userPromptBuilder, video);
            }

            // 添加关键帧信息
            if (video.containsKey("keyFrames")) {
                addKeyFramesInfo(userPromptBuilder, video);
            }
            
            userPromptBuilder.append("\n");
        }

        // 调用大模型生成报告
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPromptBuilder.toString());

        String markdownReport = videoSearchService.getMarkdownReport(systemMessage, userMessage);

        // 构建返回的报告对象
        Map<String, Object> report = new HashMap<>();
        report.put("type", "search_report");
        report.put("format", "markdown");
        report.put("content", markdownReport);
        report.put("query", query);
        report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("resultCount", filteredVideos.size());

        return report;
    }

    /**
     * 生成分析报告
     */
    private Map<String, Object> generateAnalysisReport(String query, List<Map<String, Object>> analysisResults) {
        // 构建系统提示词
        String systemPrompt = """
                你是一个专业的视频分析报告生成助手。请根据提供的视频分析结果，生成一份格式规范的Markdown格式报告。
                报告应当客观、专业、简洁，包含以下部分：

                1. 报告标题和摘要：包括分析目的、视频数量和主要发现
                2. 分析结果详情：对每个视频的分析结果进行详细说明
                3. 检测到的事件：详细描述检测到的事件，包括类型、置信度、时间点等
                4. 结论与建议：基于分析结果提供整体评估和建议措施

                请使用Markdown语法格式化报告，包括标题(#)、列表(-)、表格等元素，确保报告结构清晰、易读。
                不要添加任何额外的解释或前后文，直接返回Markdown格式的报告内容。
                """;

        // 构建用户提示词
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("请根据以下信息生成视频分析报告：\n\n");
        userPromptBuilder.append("用户查询: ").append(query).append("\n\n");
        userPromptBuilder.append("分析视频数量: ").append(analysisResults.size()).append("\n\n");

        // 添加分析结果信息
        userPromptBuilder.append("分析结果:\n");
        for (int i = 0; i < analysisResults.size(); i++) {
            Map<String, Object> result = analysisResults.get(i);
            userPromptBuilder.append(i + 1).append(". 视频ID: ").append(result.get("videoId")).append("\n");
            
            // 添加算法类型信息
            if (result.containsKey("algorithmType")) {
                String algorithmType = (String) result.get("algorithmType");
                userPromptBuilder.append("   分析方法: ");
                switch (algorithmType) {
                    case "builtin" -> userPromptBuilder.append("内置算法分析\n");
                    case "custom" -> userPromptBuilder.append("自定义算法分析\n");
                    case "llm" -> userPromptBuilder.append("大模型分析\n");
                    default -> userPromptBuilder.append(algorithmType).append("\n");
                }
            }
            
            // 添加检测到的事件信息
            if (result.containsKey("events")) {
                List<Map<String, Object>> events = (List<Map<String, Object>>) result.get("events");
                userPromptBuilder.append("   检测到的事件数量: ").append(events.size()).append("\n");
                
                for (int j = 0; j < events.size(); j++) {
                    Map<String, Object> event = events.get(j);
                    userPromptBuilder.append("   事件 ").append(j + 1).append(":\n");
                    userPromptBuilder.append("     - 类型: ").append(event.get("eventType")).append("\n");
                    userPromptBuilder.append("     - 置信度: ").append(event.get("confidence")).append("\n");
                    userPromptBuilder.append("     - 时间点: ").append(event.get("timestamp")).append("秒\n");
                    userPromptBuilder.append("     - 描述: ").append(event.get("description")).append("\n");
                    
                    if (event.containsKey("location")) {
                        userPromptBuilder.append("     - 位置: ").append(event.get("location")).append("\n");
                    }
                    
                    if (event.containsKey("severity")) {
                        userPromptBuilder.append("     - 严重程度: ").append(event.get("severity")).append("\n");
                    }
                }
            } else {
                userPromptBuilder.append("   未检测到事件\n");
            }
            
            // 添加分析结论
            if (result.containsKey("conclusion")) {
                userPromptBuilder.append("   分析结论: ").append(result.get("conclusion")).append("\n");
            }
            
            userPromptBuilder.append("\n");
        }

        // 添加总结信息
        if (analysisResults.size() > 0 && analysisResults.get(0).containsKey("summary")) {
            userPromptBuilder.append("总体分析结论:\n");
            userPromptBuilder.append(analysisResults.get(0).get("summary")).append("\n\n");
        }

        // 调用大模型生成报告
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPromptBuilder.toString());

        String markdownReport = videoSearchService.getMarkdownReport(systemMessage, userMessage);

        // 构建返回的报告对象
        Map<String, Object> report = new HashMap<>();
        report.put("type", "analysis_report");
        report.put("format", "markdown");
        report.put("content", markdownReport);
        report.put("query", query);
        report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("videoCount", analysisResults.size());

        return report;
    }

    /**
     * 生成综合报告（同时包含检索结果和分析结果）
     */
    private Map<String, Object> generateComprehensiveReport(String query, List<Map<String, Object>> filteredVideos, List<Map<String, Object>> analysisResults) {
        // 构建系统提示词
        String systemPrompt = """
                你是一个专业的视频综合报告生成助手。请根据提供的视频检索结果和分析结果，生成一份格式规范的Markdown格式综合报告。
                报告应当客观、专业、简洁，包含以下部分：

                1. 报告标题和摘要：包括查询目的、检索和分析范围
                2. 检索结果概览：检索到的视频数量和主要特点
                3. 分析结果概览：检测到的事件类型和数量
                4. 详细结果：按视频ID组织，同时展示该视频的检索信息和分析结果
                5. 综合结论与建议：基于检索和分析结果提供整体评估和建议措施

                请使用Markdown语法格式化报告，包括标题(#)、列表(-)、表格等元素，确保报告结构清晰、易读。
                不要添加任何额外的解释或前后文，直接返回Markdown格式的报告内容。
                """;

        // 构建用户提示词
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("请根据以下信息生成视频综合报告：\n\n");
        userPromptBuilder.append("用户查询: ").append(query).append("\n\n");
        userPromptBuilder.append("检索结果数量: ").append(filteredVideos.size()).append("\n");
        userPromptBuilder.append("分析结果数量: ").append(analysisResults.size()).append("\n\n");

        // 创建视频ID到检索结果的映射
        Map<String, Map<String, Object>> videoSearchMap = filteredVideos.stream()
                .collect(Collectors.toMap(
                        video -> (String) video.get("id"),
                        video -> video
                ));

        // 创建视频ID到分析结果的映射
        Map<String, Map<String, Object>> videoAnalysisMap = analysisResults.stream()
                .collect(Collectors.toMap(
                        result -> (String) result.get("videoId"),
                        result -> result,
                        (r1, r2) -> r1 // 如果有重复，保留第一个
                ));

        // 合并所有涉及的视频ID
        List<String> allVideoIds = new ArrayList<>();
        allVideoIds.addAll(videoSearchMap.keySet());
        for (String id : videoAnalysisMap.keySet()) {
            if (!allVideoIds.contains(id)) {
                allVideoIds.add(id);
            }
        }

        // 添加综合信息
        userPromptBuilder.append("综合结果:\n");
        for (int i = 0; i < allVideoIds.size(); i++) {
            String videoId = allVideoIds.get(i);
            userPromptBuilder.append(i + 1).append(". 视频ID: ").append(videoId).append("\n");
            
            // 添加检索信息
            if (videoSearchMap.containsKey(videoId)) {
                Map<String, Object> video = videoSearchMap.get(videoId);
                userPromptBuilder.append("   检索信息:\n");
                userPromptBuilder.append("     - 标题: ").append(video.get("title")).append("\n");
                userPromptBuilder.append("     - 描述: ").append(video.get("description")).append("\n");
                userPromptBuilder.append("     - 相关性评分: ").append(video.get("relevanceScore")).append("\n");
                
                // 添加视频特定属性（根据视频类型）
                if (video.containsKey("personDetection")) {
                    userPromptBuilder.append("     - 人物检测信息:\n");
                    Map<String, Object> personDetection = (Map<String, Object>) video.get("personDetection");
                    userPromptBuilder.append("       * 人物数量: ").append(personDetection.get("personCount")).append("\n");
                    
                    if (personDetection.containsKey("targetPerson")) {
                        Map<String, Object> targetPerson = (Map<String, Object>) personDetection.get("targetPerson");
                        Map<String, Object> attributes = (Map<String, Object>) targetPerson.get("attributes");
                        userPromptBuilder.append("       * 目标人物: ").append(attributes.get("gender")).append("\n");
                    }
                }
            }
            
            // 添加分析信息
            if (videoAnalysisMap.containsKey(videoId)) {
                Map<String, Object> analysis = videoAnalysisMap.get(videoId);
                userPromptBuilder.append("   分析信息:\n");
                
                if (analysis.containsKey("algorithmType")) {
                    String algorithmType = (String) analysis.get("algorithmType");
                    userPromptBuilder.append("     - 分析方法: ");
                    switch (algorithmType) {
                        case "builtin" -> userPromptBuilder.append("内置算法分析\n");
                        case "custom" -> userPromptBuilder.append("自定义算法分析\n");
                        case "llm" -> userPromptBuilder.append("大模型分析\n");
                        default -> userPromptBuilder.append(algorithmType).append("\n");
                    }
                }
                
                // 添加检测到的事件信息
                if (analysis.containsKey("events")) {
                    List<Map<String, Object>> events = (List<Map<String, Object>>) analysis.get("events");
                    userPromptBuilder.append("     - 检测到的事件数量: ").append(events.size()).append("\n");
                    
                    for (int j = 0; j < events.size(); j++) {
                        Map<String, Object> event = events.get(j);
                        userPromptBuilder.append("       * 事件 ").append(j + 1).append(": ");
                        userPromptBuilder.append(event.get("eventType")).append(" (置信度: ").append(event.get("confidence")).append(")\n");
                        userPromptBuilder.append("         时间点: ").append(event.get("timestamp")).append("秒, ");
                        userPromptBuilder.append("描述: ").append(event.get("description")).append("\n");
                    }
                } else {
                    userPromptBuilder.append("     - 未检测到事件\n");
                }
                
                // 添加分析结论
                if (analysis.containsKey("conclusion")) {
                    userPromptBuilder.append("     - 分析结论: ").append(analysis.get("conclusion")).append("\n");
                }
            }
            
            userPromptBuilder.append("\n");
        }

        // 添加总结信息
        if (!analysisResults.isEmpty() && analysisResults.get(0).containsKey("summary")) {
            userPromptBuilder.append("总体分析结论:\n");
            userPromptBuilder.append(analysisResults.get(0).get("summary")).append("\n\n");
        }

        // 调用大模型生成报告
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPromptBuilder.toString());

        String markdownReport = videoSearchService.getMarkdownReport(systemMessage, userMessage);

        // 构建返回的报告对象
        Map<String, Object> report = new HashMap<>();
        report.put("type", "comprehensive_report");
        report.put("format", "markdown");
        report.put("content", markdownReport);
        report.put("query", query);
        report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        report.put("videoCount", allVideoIds.size());
        report.put("searchResultCount", filteredVideos.size());
        report.put("analysisResultCount", analysisResults.size());

        return report;
    }

    /**
     * 生成通用报告（不依赖检索结果或分析结果）
     */
    private Map<String, Object> generateGenericReport(String query, String reportType) {
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
                """, query, reportType != null ? reportType : "general");

        // 调用大模型生成报告
        SystemMessage systemMessage = new SystemMessage(systemPrompt);
        UserMessage userMessage = new UserMessage(userPrompt);

        String markdownReport = videoSearchService.getMarkdownReport(systemMessage, userMessage);

        // 构建返回的报告对象
        Map<String, Object> report = new HashMap<>();
        report.put("type", reportType != null ? reportType : "general");
        report.put("format", "markdown");
        report.put("content", markdownReport);
        report.put("query", query);
        report.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return report;
    }

    /**
     * 添加人物检测信息到提示词
     */
    private void addPersonDetectionInfo(StringBuilder builder, Map<String, Object> video) {
        Map<String, Object> personDetection = (Map<String, Object>) video.get("personDetection");
        builder.append("   人物识别信息:\n");
        builder.append("     - 人物数量: ").append(personDetection.get("personCount")).append("\n");

        if (personDetection.containsKey("targetPerson")) {
            Map<String, Object> targetPerson = (Map<String, Object>) personDetection.get("targetPerson");
            Map<String, Object> attributes = (Map<String, Object>) targetPerson.get("attributes");

            builder.append("     - 目标人物属性:\n");
            builder.append("       * 性别: ").append(attributes.get("gender")).append("\n");

            if (attributes.containsKey("upperClothing")) {
                Map<String, Object> upperClothing = (Map<String, Object>) attributes.get("upperClothing");
                builder.append("       * 上衣: ").append(upperClothing.get("color"))
                        .append(" ").append(upperClothing.get("type"))
                        .append(" (置信度: ").append(upperClothing.get("confidence")).append(")\n");
            }

            if (attributes.containsKey("lowerClothing")) {
                Map<String, Object> lowerClothing = (Map<String, Object>) attributes.get("lowerClothing");
                builder.append("       * 裤子: ").append(lowerClothing.get("color"))
                        .append(" ").append(lowerClothing.get("type"))
                        .append(" (置信度: ").append(lowerClothing.get("confidence")).append(")\n");
            }
        }
    }

    /**
     * 添加安全事件信息到提示词
     */
    private void addSecurityEventInfo(StringBuilder builder, Map<String, Object> video) {
        List<Map<String, Object>> securityEvents = (List<Map<String, Object>>) video.get("securityEvents");
        builder.append("   安全事件信息:\n");
        builder.append("     - 检测到的事件数量: ").append(securityEvents.size()).append("\n");

        for (int i = 0; i < securityEvents.size(); i++) {
            Map<String, Object> event = securityEvents.get(i);
            builder.append("     - 事件 ").append(i + 1).append(":\n");
            builder.append("       * 类型: ").append(event.get("type")).append("\n");
            builder.append("       * 置信度: ").append(event.get("confidence")).append("\n");
            builder.append("       * 时间点: ").append(event.get("timestamp")).append("秒\n");
            builder.append("       * 严重程度: ").append(event.get("severity")).append("\n");
        }
    }

    /**
     * 添加火灾安全信息到提示词
     */
    private void addFireSafetyInfo(StringBuilder builder, Map<String, Object> video) {
        Map<String, Object> fireSafetyInfo = (Map<String, Object>) video.get("fireSafetyInfo");
        builder.append("   火灾安全信息:\n");
        builder.append("     - 烟雾检测: ").append(fireSafetyInfo.get("smokeDetected")).append("\n");
        
        if ((boolean) fireSafetyInfo.get("smokeDetected")) {
            builder.append("     - 烟雾浓度: ").append(fireSafetyInfo.get("smokeLevel")).append("\n");
            builder.append("     - 检测置信度: ").append(fireSafetyInfo.get("confidence")).append("\n");
            builder.append("     - 首次检测时间点: ").append(fireSafetyInfo.get("detectionTime")).append("秒\n");
        }
        
        builder.append("     - 火焰检测: ").append(fireSafetyInfo.get("flameDetected")).append("\n");
        builder.append("     - 风险评估: ").append(fireSafetyInfo.get("riskLevel")).append("\n");
    }

    /**
     * 添加关键帧信息到提示词
     */
    private void addKeyFramesInfo(StringBuilder builder, Map<String, Object> video) {
        List<Map<String, Object>> keyFrames = (List<Map<String, Object>>) video.get("keyFrames");
        builder.append("   关键时间点:\n");
        for (Map<String, Object> keyFrame : keyFrames) {
            builder.append("     - ").append(keyFrame.get("timestamp"))
                    .append("秒: ").append(keyFrame.get("description")).append("\n");
        }
    }
}