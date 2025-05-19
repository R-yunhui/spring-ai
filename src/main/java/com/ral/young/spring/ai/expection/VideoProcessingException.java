package com.ral.young.spring.ai.expection;

/**
 * @author renyh
 * @description 视频处理异常类
 * @date 2025/4/17 13:57
 * @since 1.0.0
 */
public class VideoProcessingException extends Exception {

	public VideoProcessingException(String message) {
		super(message);
	}

	public VideoProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

}