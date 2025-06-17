package com.ral.young.spring.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author renyh
 * @description cv 相关的聊天消息
 * @date 2025/6/17 15:59
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CvChatMessage {

	private Long chatId;

	private String prompt;

	private String model;
}
