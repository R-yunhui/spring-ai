package com.ral.young.spring.ai.constant;

import lombok.Getter;

/**
 * @author renyh
 * @description 部分功能定义
 * @date 2025/5/22 14:04
 * @since 1.0.0
 */
public class CommonConstant {

	@Getter
	public static enum ModelType {

		/**
		 * 聊天文本对话的模型
		 */
		CHAT("chat"),

		/**
		 * 嵌入模型
		 */
		EMBEDDING("embedding");


		private final String type;

		ModelType(String type) {
			this.type = type;
		}
	}

	public static enum EmbeddingType {
		/**
		 * 文本嵌入
		 */
		TEXT_EMBEDDING,
		/**
		 * 图片嵌入
		 */
		IMAGE_EMBEDDING,
		/**
		 * 视频嵌入
		 */
		VIDEO_EMBEDDING
	}

	;
}
