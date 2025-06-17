package com.ral.young.spring.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author renyh
 * @description 用于提供 CV 能力配置流程中所需的各种工具函数。
 * @date 2025/6/17 15:41
 * @since 1.0.0
 */
@Slf4j
@Service
public class CvToolService {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Tool(name = "find_suitable_models", description = "根据用户的自然语言需求，从模型库中找出最匹配的候选 CV 模型。")
	public String findSuitableModels(
			@ToolParam(description = "用户的原始需求描述，例如：'检测人员入侵' 或 '识别火灾和烟雾'") String taskDescription) {
		log.info("调用工具 [find_suitable_models]，输入参数: {}", taskDescription);

		// 模拟返回的丰富的模型数据
		List<Map<String, String>> models = List.of(
				Map.of("model_id", "yolov8-traffic", "description", "高精度通用交通场景物体检测模型"),
				Map.of("model_id", "animal-detection-v1", "description", "专门用于检测常见动物的模型"),
				Map.of("model_id", "fire-smoke-detection-v2", "description", "专门用于烟火检测的垂直领域模型"),
				Map.of("model_id", "license-plate-recognition-cn", "description", "识别中国大陆车辆牌照的模型"),
				Map.of("model_id", "mobilenet-ssd-lite", "description", "轻量级高帧率移动端检测模型"),
				Map.of("model_id", "human-attribute-recognition", "description", "识别人物属性的模型，如性别、年龄、穿着")
		);

		log.info("工具 [find_suitable_models] 返回: {}", models);
		return toJson(models);
	}

	@Tool(name = "get_model_labels", description = "获取指定 CV 模型所支持的所有可检测标签。")
	public String getModelLabels(
			@ToolParam(description = "需要查询的CV模型的唯一ID，例如：'yolov8-traffic'") String modelId) {
		log.info("调用工具 [get_model_labels]，输入参数: {}", modelId);

		// 模拟不同模型支持的丰富的标签
		Map<String, List<String>> labelsDb = Map.of(
				"yolov8-traffic", List.of("person", "car", "truck", "bus", "bicycle", "motorcycle", "traffic_light", "fire_hydrant", "stop_sign"),
				"animal-detection-v1", List.of("dog", "cat", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "bird"),
				"fire-smoke-detection-v2", List.of("fire", "smoke", "smog"),
				"license-plate-recognition-cn", List.of("plate_blue", "plate_green_new_energy", "plate_yellow", "plate_black", "plate_white"),
				"mobilenet-ssd-lite", List.of("person", "bicycle", "car", "motorcycle", "bus", "train"),
				"human-attribute-recognition", List.of("male", "female", "child", "adult", "senior", "wearing_glasses", "wearing_hat", "t-shirt", "jacket")
		);

		List<String> labels = labelsDb.getOrDefault(modelId, List.of("unknown_model_id"));
		log.info("工具 [get_model_labels] 返回: {}", labels);
		return toJson(labels);
	}

	@Tool(name = "create_deployment_template", description = "在用户确认后，根据指定的模型和标签生成最终的能力部署模板。这是流程的最后一步。")
	public String createDeploymentTemplate(
			@ToolParam(description = "用户最终选择的CV模型的唯一ID") String modelId,
			@ToolParam(description = "用户最终选择的、希望模型检测的标签列表") List<String> labels,
			@ToolParam(description = "此参数必须为 true，函数才能成功执行。这是安全控制的关键。在调用此函数前，必须先和用户进行确认。") boolean userHasConfirmed) {
		log.info("调用工具 [create_deployment_template]，输入参数: modelId={}, labels={}, userHasConfirmed={}", modelId, labels, userHasConfirmed);

		// 关键安全校验：严格检查 userHasConfirmed 标志
		if (!userHasConfirmed) {
			log.warn("安全护栏触发：用户未确认，已拒绝生成模板。");
			return toJson(Map.of("error", "操作被拒绝，用户必须先进行确认才能生成模板。请提示用户进行确认。"));
		}

		// 模拟生成 YAML 格式的部署模板
		String template =
				"""
						apiVersion: apps/v1
						kind: Deployment
						metadata:
						  name: cv-ability-deployment-%s
						spec:
						  replicas: 1
						  selector:
							matchLabels:
							  app: cv-ability
						  template:
							metadata:
							  labels:
								app: cv-ability
							spec:
							  containers:
							  - name: inference-server
								image: registry/inference-server:1.0
								args: [
								  "--model_id=%s",
								  "--labels=%s"
								]
						""".formatted(modelId, modelId, String.join(",", labels));

		log.info("工具 [create_deployment_template] 成功生成模板。");
		return toJson(Map.of("templateContent", template));
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			log.error("JSON 序列化失败", e);
			return "{\"error\": \"序列化返回结果时出错\"}";
		}
	}
}
