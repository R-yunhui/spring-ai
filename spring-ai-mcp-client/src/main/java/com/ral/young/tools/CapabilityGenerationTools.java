package com.ral.young.tools;

import com.ral.young.dto.request.AlgorithmAnalysisRequest;
import com.ral.young.dto.request.CapabilityGenerationRequest;
import com.ral.young.dto.response.AlgorithmAnalysisResponse;
import com.ral.young.dto.response.CapabilityGenerationResponse;
import com.ral.young.enums.TaskStatusEnum;
import com.ral.young.manager.TaskManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author renyh
 * @description 能力生成工具
 * @date 2025/7/25 10:50
 * @since 1.0.0
 */
@Service
@Slf4j
@SuppressWarnings("preview")
public class CapabilityGenerationTools {

	@Resource
	private TaskManager taskManager;

	@Resource
	private AlgorithmAnalysisTools algorithmAnalysisTools;

	/**
	 * 能力生成工具
	 * 创建新的分析能力
	 *
	 * @param request 能力生成请求
	 * @return 能力生成响应
	 */
	@Tool(description = "创建新的分析能力，这是一个异步任务，会生成一个新的分析算法。")
	public CapabilityGenerationResponse generateCapability(CapabilityGenerationRequest request) {
		log.info("调用能力生成工具，能力名称: {}, 视频数量: {}", request.getCapabilityName(), request.getVideoIds().size());

		// 生成能力ID
		String capabilityId = STR."CAP_\{UUID.randomUUID().toString().substring(0, 8).toUpperCase()}";
		// 生成算法类型
		String algorithmType = STR."CUSTOM_\{capabilityId}";

		// 提交异步任务
		Long taskId = taskManager.submitTask(() -> {
			log.info("开始生成新能力: {}, 能力ID: {}", request.getCapabilityName(), capabilityId);

			try {
				// 模拟耗时操作
				Thread.sleep(8000);

				log.info("能力生成完成: {}, 能力ID: {}", request.getCapabilityName(), capabilityId);

				// 能力生成完成后，自动触发算法下发
				AlgorithmAnalysisRequest analysisRequest = AlgorithmAnalysisRequest.builder()
						.videoIds(request.getVideoIds())
						.algorithmType(algorithmType)
						.analysisParams(STR."{\"capabilityId\": \"\{capabilityId}\", \"capabilityName\": \"\{request.getCapabilityName()}\"}")
						.chatId(request.getChatId())
						.build();

				// 调用算法下发工具
				AlgorithmAnalysisResponse analysisResponse = algorithmAnalysisTools.dispatchAlgorithmAnalysis(analysisRequest);
				log.info("自动触发算法下发，任务ID: {}", analysisResponse.getTaskId());

			} catch (InterruptedException e) {
				log.error("能力生成任务被中断", e);
				throw new RuntimeException("能力生成任务被中断", e);
			}
		});

		// 构建响应
		return CapabilityGenerationResponse.builder()
				.taskId(taskId)
				.capabilityId(capabilityId)
				.capabilityName(request.getCapabilityName())
				.status(TaskStatusEnum.PENDING)
				.estimatedCompletionTime(30)
				.chatId(request.getChatId())
				.build();
	}
}