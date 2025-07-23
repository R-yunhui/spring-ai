package com.ral.young.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.Serializable;
import java.util.List;

/**
 * @author renyh
 * @description 能力生成请求
 * @date 2025/7/25 10:45
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityGenerationRequest implements Serializable {

	/**
	 * 用户查询
	 */
	@ToolParam(description = "用户的原始查询全文，用于理解用户需求")
	private String query;

	/**
	 * 视频ID列表
	 */
	@ToolParam(description = "需要分析的视频ID列表")
	private List<String> videoIds;

	/**
	 * 能力描述
	 */
	@ToolParam(description = "对需要生成的分析能力的详细描述")
	private String capabilityDescription;

	/**
	 * 能力名称
	 */
	@ToolParam(description = "新生成能力的名称")
	private String capabilityName;

	/**
	 * 聊天ID
	 */
	@ToolParam(description = "当前会话的ID，用于后续通知")
	private Long chatId;
}