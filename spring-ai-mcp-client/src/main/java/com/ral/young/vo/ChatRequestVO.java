package com.ral.young.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author renyh
 * @description 聊天请求参数
 * @date 2025/7/1 10:37
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatRequestVO {

	private String prompt;

	private Long requestId;
}
