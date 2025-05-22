package com.ral.young.spring.ai.controller;

import com.ral.young.spring.ai.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

/**
 * @author renyh
 * @description 视频抽帧定义
 * @date 2025/4/18 16:42
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/video")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

	private final VideoService videoService;

	/**
	 * SSE 接口：上传视频并流式返回抽帧结果
	 * @param file 视频文件
	 * @param interval 抽帧间隔 (秒)
	 * @param batchSize 每批返回的文件数量
	 * @return SseEmitter
	 */
	@PostMapping("/extract/sse")
	public SseEmitter extractFramesSse(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "interval", defaultValue = "1") int interval,
			@RequestParam(value = "batchSize", defaultValue = "10") int batchSize) {

		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Timeout 设置为无限长
		log.info("SSE 连接建立，interval={}, batchSize={}", interval, batchSize);

		CompletableFuture.runAsync(() -> {
			try {
				videoService.extractFramesSse(file, interval, batchSize, emitter);
			} catch (Exception e) {
				log.error("SSE 处理异常", e);
				emitter.completeWithError(e); // 确保通知错误
			}
		});

		emitter.onTimeout(() -> log.warn("SSE emitter timeout."));
		emitter.onError(e -> log.error("SSE emitter error.", e));
		emitter.onCompletion(() -> log.info("SSE emitter completed."));
		return emitter;
	}
}
