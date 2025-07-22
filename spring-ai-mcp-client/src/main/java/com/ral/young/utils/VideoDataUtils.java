package com.ral.young.utils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author renyh
 * @description 视频数据工具类，提供共享的数据生成和工具方法
 * @date 2025/7/1 10:00
 * @since 1.0.0
 */
public class VideoDataUtils {
    
    // 视频缓存，模拟数据库
    private static final ConcurrentHashMap<String, Map<String, Object>> VIDEO_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 根据ID获取视频信息
     */
    public static Map<String, Object> getVideoById(String id) {
        return VIDEO_CACHE.getOrDefault(id, null);
    }
    
    /**
     * 创建人物相关视频
     */
    public static Map<String, Object> createPersonVideo(String id, String title, String description, double relevance) {
        Map<String, Object> video = createBaseVideo(id, title, description, relevance);
        video.put("type", "person");
        
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
        
        // 缓存视频
        VIDEO_CACHE.put(id, video);
        
        return video;
    }
    
    /**
     * 创建安全事件相关视频
     */
    public static Map<String, Object> createSecurityVideo(String id, String title, String description, double relevance) {
        Map<String, Object> video = createBaseVideo(id, title, description, relevance);
        video.put("type", "security");
        
        // 添加安全事件信息
        List<Map<String, Object>> securityEvents = new ArrayList<>();
        securityEvents.add(Map.of(
                "type", "unauthorized_access",
                "confidence", 0.92,
                "timestamp", 35,
                "severity", "high",
                "description", "未授权人员尝试进入受限区域"
        ));
        
        if (id.equals("vid_101")) {
            securityEvents.add(Map.of(
                    "type", "tampering",
                    "confidence", 0.85,
                    "timestamp", 67,
                    "severity", "high",
                    "description", "检测到门锁被撬动"
            ));
        } else if (id.equals("vid_104")) {
            securityEvents.add(Map.of(
                    "type", "perimeter_breach",
                    "confidence", 0.88,
                    "timestamp", 42,
                    "severity", "high",
                    "description", "检测到围墙被攀爬"
            ));
        }
        
        video.put("securityEvents", securityEvents);
        
        // 添加关键帧信息
        List<Map<String, Object>> keyFrames = new ArrayList<>();
        keyFrames.add(Map.of(
                "timestamp", 35,
                "eventType", "unauthorized_access",
                "description", "可疑人员出现在画面中"
        ));
        keyFrames.add(Map.of(
                "timestamp", 67,
                "eventType", "tampering",
                "description", "可疑人员接触门锁"
        ));
        video.put("keyFrames", keyFrames);
        
        // 缓存视频
        VIDEO_CACHE.put(id, video);
        
        return video;
    }
    
    /**
     * 创建火灾安全相关视频
     */
    public static Map<String, Object> createFireSafetyVideo(String id, String title, String description, double relevance) {
        Map<String, Object> video = createBaseVideo(id, title, description, relevance);
        video.put("type", "fire_safety");
        
        // 添加火灾安全信息
        boolean smokeDetected = true;
        boolean flameDetected = id.equals("vid_201") || id.equals("vid_204");
        
        Map<String, Object> fireSafetyInfo = new HashMap<>();
        fireSafetyInfo.put("smokeDetected", smokeDetected);
        fireSafetyInfo.put("smokeLevel", id.equals("vid_201") ? "high" : "medium");
        fireSafetyInfo.put("flameDetected", flameDetected);
        fireSafetyInfo.put("confidence", 0.94);
        fireSafetyInfo.put("detectionTime", 28);
        fireSafetyInfo.put("riskLevel", flameDetected ? "critical" : "high");
        
        video.put("fireSafetyInfo", fireSafetyInfo);
        
        // 添加关键帧信息
        List<Map<String, Object>> keyFrames = new ArrayList<>();
        keyFrames.add(Map.of(
                "timestamp", 28,
                "eventType", "smoke_detection",
                "description", "烟雾开始出现在画面中"
        ));
        
        if (flameDetected) {
            keyFrames.add(Map.of(
                    "timestamp", 45,
                    "eventType", "flame_detection",
                    "description", "小火苗出现"
            ));
        }
        
        video.put("keyFrames", keyFrames);
        
        // 缓存视频
        VIDEO_CACHE.put(id, video);
        
        return video;
    }
    
    /**
     * 创建通用视频
     */
    public static Map<String, Object> createGenericVideo(String id, String title, String description, double relevance) {
        Map<String, Object> video = createBaseVideo(id, title, description, relevance);
        video.put("type", "generic");
        
        // 添加关键帧信息
        List<Map<String, Object>> keyFrames = new ArrayList<>();
        keyFrames.add(Map.of(
                "timestamp", 30,
                "description", "场景概览"
        ));
        keyFrames.add(Map.of(
                "timestamp", 90,
                "description", "人员活动高峰"
        ));
        video.put("keyFrames", keyFrames);
        
        // 缓存视频
        VIDEO_CACHE.put(id, video);
        
        return video;
    }
    
    /**
     * 创建基础视频对象
     */
    private static Map<String, Object> createBaseVideo(String id, String title, String description, double relevance) {
        Map<String, Object> video = new HashMap<>();
        video.put("id", id);
        video.put("title", title);
        video.put("description", description);
        video.put("duration", 60 + (int) (Math.random() * 120));
        video.put("relevanceScore", relevance);
        video.put("timestamp", "2025-07-10T14:30:00Z");
        video.put("source", "监控摄像头");
        return video;
    }
    
    /**
     * 创建分析结果
     */
    public static Map<String, Object> createAnalysisResult(String videoId, String algorithmType, List<Map<String, Object>> events) {
        Map<String, Object> result = new HashMap<>();
        result.put("videoId", videoId);
        result.put("algorithmType", algorithmType);
        result.put("events", events);
        result.put("timestamp", System.currentTimeMillis());
        
        // 添加结论
        if (events != null && !events.isEmpty()) {
            boolean hasCritical = false;
            boolean hasHigh = false;
            
            for (Map<String, Object> event : events) {
                String severity = (String) event.getOrDefault("severity", "medium");
                if ("critical".equals(severity)) {
                    hasCritical = true;
                } else if ("high".equals(severity)) {
                    hasHigh = true;
                }
            }
            
            if (hasCritical) {
                result.put("conclusion", "检测到严重安全事件，需要立即处理");
            } else if (hasHigh) {
                result.put("conclusion", "检测到高风险事件，建议尽快处理");
            } else {
                result.put("conclusion", "检测到一般事件，建议关注");
            }
        } else {
            result.put("conclusion", "未检测到异常事件");
        }
        
        return result;
    }
    
    /**
     * 生成分析摘要
     */
    public static Map<String, Object> generateAnalysisSummary(List<Map<String, Object>> analysisResults, String analysisType, List<String> eventTypes) {
        Map<String, Object> summary = new HashMap<>();
        
        // 统计事件类型和数量
        Map<String, Integer> eventCounts = new HashMap<>();
        int totalEvents = 0;
        int criticalCount = 0;
        int highCount = 0;
        
        for (Map<String, Object> result : analysisResults) {
            if (result.containsKey("events")) {
                List<Map<String, Object>> events = (List<Map<String, Object>>) result.get("events");
                totalEvents += events.size();
                
                for (Map<String, Object> event : events) {
                    String eventType = (String) event.get("eventType");
                    eventCounts.put(eventType, eventCounts.getOrDefault(eventType, 0) + 1);
                    
                    String severity = (String) event.getOrDefault("severity", "medium");
                    if ("critical".equals(severity)) {
                        criticalCount++;
                    } else if ("high".equals(severity)) {
                        highCount++;
                    }
                }
            }
        }
        
        // 生成摘要信息
        summary.put("totalEvents", totalEvents);
        summary.put("eventCounts", eventCounts);
        summary.put("criticalCount", criticalCount);
        summary.put("highCount", highCount);
        
        // 生成总体结论
        StringBuilder conclusion = new StringBuilder();
        
        if (totalEvents == 0) {
            conclusion.append("分析完成，未检测到任何异常事件。");
        } else {
            conclusion.append(String.format("分析完成，共检测到 %d 个事件", totalEvents));
            
            if (criticalCount > 0) {
                conclusion.append(String.format("，其中包含 %d 个严重风险事件", criticalCount));
            }
            
            if (highCount > 0) {
                conclusion.append(String.format("，%d 个高风险事件", highCount));
            }
            
            conclusion.append("。建议");
            
            if (criticalCount > 0) {
                conclusion.append("立即处理严重风险事件");
            } else if (highCount > 0) {
                conclusion.append("尽快处理高风险事件");
            } else {
                conclusion.append("关注检测到的事件");
            }
            
            conclusion.append("。");
        }
        
        summary.put("conclusion", conclusion.toString());
        
        return summary;
    }
    
    /**
     * 匹配内置算法
     */
    public static Map<String, Object> matchBuiltinAlgorithms(String analysisType, List<String> eventTypes) {
        List<Map<String, Object>> algorithms = new ArrayList<>();
        
        // 模拟算法匹配逻辑
        if (eventTypes != null && !eventTypes.isEmpty()) {
            String eventTypesStr = String.join(" ", eventTypes);
            
            if (eventTypesStr.contains("入侵") || eventTypesStr.contains("异常行为")) {
                algorithms.add(Map.of(
                        "id", "algo-001",
                        "name", "入侵检测算法",
                        "type", "security",
                        "confidence", 0.95,
                        "description", "检测视频中的入侵和异常行为"
                ));
            }
            
            if (eventTypesStr.contains("烟") || eventTypesStr.contains("火") || eventTypesStr.contains("烟雾")) {
                algorithms.add(Map.of(
                        "id", "algo-002",
                        "name", "烟火检测算法",
                        "type", "fire_safety",
                        "confidence", 0.97,
                        "description", "检测视频中的烟雾和火焰"
                ));
            }
            
            if (eventTypesStr.contains("人") || eventTypesStr.contains("行人") || eventTypesStr.contains("人物")) {
                algorithms.add(Map.of(
                        "id", "algo-003",
                        "name", "人物检测算法",
                        "type", "person",
                        "confidence", 0.94,
                        "description", "检测和识别视频中的人物"
                ));
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("algorithms", algorithms);
        result.put("count", algorithms.size());
        
        return result;
    }
    
    /**
     * 生成自定义算法
     */
    public static Map<String, Object> generateCustomAlgorithm(String task, List<String> eventTypes, Map<String, Object> sampleData) {
        // 模拟算法生成逻辑
        boolean success = Math.random() > 0.3; // 70%的成功率
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        
        if (success) {
            Map<String, Object> algorithm = new HashMap<>();
            algorithm.put("id", "custom-" + System.currentTimeMillis() % 1000);
            algorithm.put("name", "自定义" + String.join("和", eventTypes) + "检测算法");
            algorithm.put("type", "custom");
            algorithm.put("confidence", 0.85);
            algorithm.put("description", "针对" + task + "定制的算法");
            
            result.put("algorithm", algorithm);
            result.put("message", "算法生成成功");
        } else {
            result.put("message", "无法生成满足要求的算法");
            result.put("reason", "缺乏足够的训练数据或任务过于复杂");
        }
        
        return result;
    }
    
    /**
     * 执行算法
     */
    public static Map<String, Object> executeAlgorithm(String algorithmId, String videoId) {
        // 获取视频信息
        Map<String, Object> video = getVideoById(videoId);
        if (video == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "找不到指定ID的视频");
            return errorResult;
        }
        
        // 根据算法ID和视频类型生成分析结果
        String videoType = (String) video.get("type");
        List<Map<String, Object>> events = new ArrayList<>();
        
        if (algorithmId.startsWith("algo-001") || algorithmId.contains("入侵") || algorithmId.contains("异常")) {
            // 入侵检测算法
            if ("security".equals(videoType)) {
                // 从视频中提取安全事件
                if (video.containsKey("securityEvents")) {
                    List<Map<String, Object>> securityEvents = (List<Map<String, Object>>) video.get("securityEvents");
                    for (Map<String, Object> secEvent : securityEvents) {
                        events.add(Map.of(
                                "eventType", secEvent.get("type"),
                                "confidence", secEvent.get("confidence"),
                                "timestamp", secEvent.get("timestamp"),
                                "description", secEvent.get("description"),
                                "severity", secEvent.get("severity")
                        ));
                    }
                }
            } else if (video.get("description").toString().contains("尝试") || 
                       video.get("description").toString().contains("陌生人") ||
                       video.get("description").toString().contains("未授权")) {
                // 根据描述判断可能存在的安全事件
                events.add(Map.of(
                        "eventType", "suspicious_activity",
                        "confidence", 0.82,
                        "timestamp", 40,
                        "description", "检测到可疑活动",
                        "severity", "medium"
                ));
            }
        } else if (algorithmId.startsWith("algo-002") || algorithmId.contains("烟") || algorithmId.contains("火")) {
            // 烟火检测算法
            if ("fire_safety".equals(videoType)) {
                // 从视频中提取火灾安全信息
                if (video.containsKey("fireSafetyInfo")) {
                    Map<String, Object> fireInfo = (Map<String, Object>) video.get("fireSafetyInfo");
                    if ((boolean) fireInfo.get("smokeDetected")) {
                        events.add(Map.of(
                                "eventType", "smoke_detection",
                                "confidence", fireInfo.get("confidence"),
                                "timestamp", fireInfo.get("detectionTime"),
                                "description", "检测到" + fireInfo.get("smokeLevel") + "浓度烟雾",
                                "severity", "high"
                        ));
                    }
                    
                    if ((boolean) fireInfo.get("flameDetected")) {
                        events.add(Map.of(
                                "eventType", "flame_detection",
                                "confidence", fireInfo.get("confidence"),
                                "timestamp", (int) fireInfo.get("detectionTime") + 15,
                                "description", "检测到火焰",
                                "severity", "critical"
                        ));
                    }
                }
            } else if (video.get("description").toString().contains("烟雾") || 
                       video.get("description").toString().contains("火")) {
                // 根据描述判断可能存在的火灾安全事件
                events.add(Map.of(
                        "eventType", "smoke_detection",
                        "confidence", 0.78,
                        "timestamp", 35,
                        "description", "检测到可能的烟雾",
                        "severity", "medium"
                ));
            }
        } else if (algorithmId.startsWith("algo-003") || algorithmId.contains("人")) {
            // 人物检测算法
            if ("person".equals(videoType)) {
                // 从视频中提取人物信息
                if (video.containsKey("personDetection")) {
                    Map<String, Object> personInfo = (Map<String, Object>) video.get("personDetection");
                    events.add(Map.of(
                            "eventType", "person_detection",
                            "confidence", 0.95,
                            "timestamp", 15,
                            "description", "检测到" + personInfo.get("personCount") + "个人物",
                            "severity", "low"
                    ));
                    
                    if (personInfo.containsKey("targetPerson")) {
                        Map<String, Object> targetPerson = (Map<String, Object>) personInfo.get("targetPerson");
                        Map<String, Object> attributes = (Map<String, Object>) targetPerson.get("attributes");
                        
                        events.add(Map.of(
                                "eventType", "target_person_detection",
                                "confidence", targetPerson.get("confidence"),
                                "timestamp", 45,
                                "description", "检测到目标人物：" + attributes.get("gender") + 
                                              "，穿着" + ((Map<String, Object>)attributes.get("upperClothing")).get("color") + 
                                              ((Map<String, Object>)attributes.get("upperClothing")).get("type"),
                                "severity", "low"
                        ));
                    }
                }
            } else if (video.get("description").toString().contains("人") || 
                       video.get("description").toString().contains("男") || 
                       video.get("description").toString().contains("女")) {
                // 根据描述判断可能存在的人物
                events.add(Map.of(
                        "eventType", "person_detection",
                        "confidence", 0.85,
                        "timestamp", 20,
                        "description", "检测到人物",
                        "severity", "low"
                ));
            }
        } else if (algorithmId.startsWith("custom-")) {
            // 自定义算法，生成一些通用结果
            String description = (String) video.get("description");
            
            if (description.contains("入侵") || description.contains("尝试") || description.contains("陌生人")) {
                events.add(Map.of(
                        "eventType", "security_event",
                        "confidence", 0.83,
                        "timestamp", 30,
                        "description", "检测到安全相关事件",
                        "severity", "medium"
                ));
            }
            
            if (description.contains("烟") || description.contains("火")) {
                events.add(Map.of(
                        "eventType", "fire_safety_event",
                        "confidence", 0.81,
                        "timestamp", 40,
                        "description", "检测到火灾安全相关事件",
                        "severity", "high"
                ));
            }
            
            if (events.isEmpty()) {
                // 如果没有检测到特定事件，添加一个通用事件
                events.add(Map.of(
                        "eventType", "general_event",
                        "confidence", 0.75,
                        "timestamp", 25,
                        "description", "检测到一般事件",
                        "severity", "low"
                ));
            }
        }
        
        return createAnalysisResult(videoId, algorithmId.startsWith("custom-") ? "custom" : "builtin", events);
    }
    
    /**
     * 分析视频切片
     */
    public static Map<String, Object> analyzeVideoBySlicing(String videoId, String query, int sliceInterval) {
        // 获取视频信息
        Map<String, Object> video = getVideoById(videoId);
        if (video == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "找不到指定ID的视频");
            return errorResult;
        }
        
        // 根据视频内容和查询生成分析结果
        List<Map<String, Object>> events = new ArrayList<>();
        String description = (String) video.get("description");
        
        if (query.contains("入侵") && (description.contains("尝试") || description.contains("陌生人"))) {
            events.add(Map.of(
                    "eventType", "unauthorized_access",
                    "confidence", 0.87,
                    "timestamp", 35,
                    "description", "大模型分析：检测到未授权人员进入受限区域",
                    "severity", "high",
                    "location", "视频画面右下角"
            ));
        }
        
        if (query.contains("烟") && description.contains("烟雾")) {
            events.add(Map.of(
                    "eventType", "smoke_detection",
                    "confidence", 0.89,
                    "timestamp", 28,
                    "description", "大模型分析：检测到明显烟雾",
                    "severity", "high",
                    "location", "视频画面中央"
            ));
        }
        
        if (query.contains("异常") && description.contains("徘徊")) {
            events.add(Map.of(
                    "eventType", "suspicious_behavior",
                    "confidence", 0.84,
                    "timestamp", 42,
                    "description", "大模型分析：检测到可疑徘徊行为",
                    "severity", "medium",
                    "location", "视频画面左侧"
            ));
        }
        
        if (query.contains("人物") && description.contains("男")) {
            events.add(Map.of(
                    "eventType", "person_detection",
                    "confidence", 0.92,
                    "timestamp", 15,
                    "description", "大模型分析：检测到符合描述的人物",
                    "severity", "low",
                    "location", "视频画面中央"
            ));
        }
        
        return createAnalysisResult(videoId, "llm", events);
    }
}