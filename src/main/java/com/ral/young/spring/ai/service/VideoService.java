package com.ral.young.spring.ai.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.google.common.collect.Lists;
import com.ral.young.spring.ai.dto.FileDTO;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author renyh
 * @description 抽帧服务
 * @date 2025/4/18 16:43
 * @since 1.0.0
 */
@Service
@Slf4j
public class VideoService {

	// 用于异步任务状态跟踪 (简单实现，生产环境可能需要更健壮的存储)
	private final Map<String, AsyncTaskStatus> asyncTaskStore = new ConcurrentHashMap<>();
	// 用于执行核心抽帧逻辑的线程池
	private final ExecutorService frameExtractorExecutor;

	// 构造函数，初始化线程池 (可配置)
	public VideoService() {
		int poolSize = Runtime.getRuntime().availableProcessors();
		this.frameExtractorExecutor = new ThreadPoolExecutor(
				poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(1000),
				new ThreadFactoryBuilder().setNamePrefix("frame-extractor-").build(),
				new ThreadPoolExecutor.CallerRunsPolicy()
		);

		// 预热可以在需要时调用，或者在配置类中
		((ThreadPoolExecutor) this.frameExtractorExecutor).prestartAllCoreThreads();
		log.info("VideoService frameExtractorExecutor initialized with pool size: {}", poolSize);
		// 设置 FFmpeg 日志级别 (全局设置，只需一次)
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
	}

	// --- 异步接口相关 ---

	/**
	 * 启动异步视频抽帧任务
	 *
	 * @param file     视频文件
	 * @param interval 抽帧间隔 (秒)
	 * @return 任务ID
	 * @throws IOException 文件处理异常
	 */
	public String startFrameExtractionAsync(MultipartFile file, int interval) throws IOException {
		String taskId = UUID.randomUUID().toString();
		Path tempFilePath = saveTemporaryFile(file, taskId);

		AsyncTaskStatus status = new AsyncTaskStatus(taskId, AsyncTaskState.PENDING, null, null);
		asyncTaskStore.put(taskId, status);

		// 异步执行实际的抽帧逻辑
		extractFramesAsyncInternal(taskId, tempFilePath.toString(), interval);

		log.info("异步任务已启动，Task ID: {}", taskId);
		return taskId;
	}

	/**
	 * 内部异步方法，执行抽帧并更新状态
	 *
	 * @param taskId    任务ID
	 * @param videoPath 视频文件临时路径
	 * @param interval  抽帧间隔
	 */
	@Async // 使用 Spring 的异步执行能力 (需要在配置类加 @EnableAsync)
	public void extractFramesAsyncInternal(String taskId, String videoPath, int interval) {
		log.info("开始执行异步任务 ID: {}", taskId);
		updateTaskStatus(taskId, AsyncTaskState.RUNNING, "开始处理...");
		List<String> resultFilePaths;
		try {
			// 注意：这里直接调用核心抽帧逻辑，它会返回结果列表
			// outputPath 需要规划，例如每个任务一个子目录
			Path outputBaseDir = Paths.get("output_frames"); // 可配置
			Files.createDirectories(outputBaseDir);
			Path taskOutputDir = outputBaseDir.resolve(taskId);
			Files.createDirectories(taskOutputDir);

			List<FileDTO> resultFiles = extractFramesCore(videoPath, taskOutputDir.toString(), interval, null, 0);
			resultFilePaths = resultFiles.stream().map(FileDTO::getFileName).toList();

			updateTaskStatus(taskId, AsyncTaskState.COMPLETED, "处理完成", resultFilePaths);
			log.info("异步任务 ID: {} 完成", taskId);

		} catch (Exception e) {
			log.error("异步任务 ID: {} 执行失败", taskId, e);
			updateTaskStatus(taskId, AsyncTaskState.FAILED, "处理失败: " + e.getMessage());
		} finally {
			// 清理临时文件
			cleanupTemporaryFile(videoPath);
		}
	}

	/**
	 * 获取异步任务状态
	 *
	 * @param taskId 任务ID
	 * @return 任务状态
	 */
	public AsyncTaskStatus getAsyncTaskStatus(String taskId) {
		return asyncTaskStore.get(taskId);
	}

	private void updateTaskStatus(String taskId, AsyncTaskState state, String message) {
		updateTaskStatus(taskId, state, message, null);
	}

	private void updateTaskStatus(String taskId, AsyncTaskState state, String message, List<String> results) {
		AsyncTaskStatus currentStatus = asyncTaskStore.get(taskId);
		if (currentStatus != null) {
			// 保留之前的结果（如果新状态是中间状态）
			List<String> finalResults = (results != null) ? results : currentStatus.resultFilePaths();
			asyncTaskStore.put(taskId, new AsyncTaskStatus(taskId, state, message, finalResults));
		} else {
			log.warn("尝试更新不存在的任务状态: {}", taskId);
		}
	}

	// --- SSE 接口相关 ---

	/**
	 * 通过 SSE 流式处理视频抽帧
	 *
	 * @param file      视频文件
	 * @param interval  抽帧间隔
	 * @param batchSize 每批次发送的文件数量
	 * @param emitter   SSE Emitter
	 */
	public void extractFramesSse(MultipartFile file, int interval, int batchSize, SseEmitter emitter) {
		String taskId = "sse-" + UUID.randomUUID().toString().substring(0, 8); // 简短标识
		Path tempFilePath = null;
		try {
			tempFilePath = saveTemporaryFile(file, taskId);
			log.info("SSE 任务 {} 开始，处理文件: {}", taskId, tempFilePath);

			// outputPath 需要规划
			Path outputBaseDir = Paths.get("output_frames");
			Files.createDirectories(outputBaseDir);
			Path taskOutputDir = outputBaseDir.resolve(taskId);
			Files.createDirectories(taskOutputDir);

			// 定义 SSE 发送逻辑 (作为回调)
			Consumer<List<FileDTO>> sseBatchProcessor = batch -> {
				try {
					if (!batch.isEmpty()) {
						// 发送相对路径或处理后的信息更佳，这里为简化发送绝对路径
						emitter.send(SseEmitter.event().name("frame-batch").data(batch));
						log.debug("SSE任务 {} 发送批次，数量: {}", taskId, batch.size());
					}
				} catch (IOException e) {
					log.error("SSE任务 {} 发送失败: {}", taskId, e.getMessage());
					// 可以尝试关闭 emitter 或标记错误
					throw new RuntimeException("SSE send error", e);
				}
			};

			// 调用核心抽帧逻辑，传入 SSE 回调
			extractFramesCore(tempFilePath.toString(), taskOutputDir.toString(), interval, sseBatchProcessor, batchSize);

			emitter.complete(); // 正常完成
			log.info("SSE 任务 {} 完成", taskId);

		} catch (Exception e) {
			log.error("SSE 任务 {} 失败", taskId, e);
			try {
				emitter.completeWithError(e); // 通知客户端错误
			} catch (Exception ignore) {
				log.warn("SSE 任务 {} 通知错误时失败", taskId);
			}
		} finally {
			if (tempFilePath != null) {
				cleanupTemporaryFile(tempFilePath.toString());
			}
		}
	}

	// --- 核心抽帧逻辑 (重构自 extractFrames) ---

	/**
	 * 核心视频抽帧方法
	 *
	 * @param videoPath      视频文件路径
	 * @param outputDir      输出目录
	 * @param interval       抽帧间隔 (秒)
	 * @param batchProcessor (可选) 批处理回调，用于 SSE。如果提供，则按 batchSize 调用此回调，最终返回空列表。
	 * @param batchSize      批处理大小 (仅当 batchProcessor 不为 null 时有效)
	 * @return 如果 batchProcessor 为 null，返回所有提取的文件列表；否则返回空列表。
	 * @throws Exception 处理异常
	 */
	private List<FileDTO> extractFramesCore(String videoPath, String outputDir, int interval,
											Consumer<List<FileDTO>> batchProcessor, int batchSize) throws Exception {
		long startTime = System.currentTimeMillis();
		log.info("核心处理开始: 视频={}, 输出={}, 间隔={}", videoPath, outputDir, interval);

		// 使用 ConcurrentSkipListMap 保证顺序，如果不需要批处理回调，则用它收集结果
		ConcurrentSkipListMap<Integer, FileDTO> sortedFrameFiles = (batchProcessor == null) ? new ConcurrentSkipListMap<>() : null;
		// 用于批处理回调的临时列表
		List<FileDTO> currentBatch = (batchProcessor != null) ? new ArrayList<>(batchSize) : null;
		// 用于同步批处理列表的锁
		Object batchLock = new Object();

		try (FFmpegFrameGrabber initialGrabber = new FFmpegFrameGrabber(videoPath)) {
			// 视频格式校验: 尝试 start() 并获取信息
			try {
				initialGrabber.start();
			} catch (Exception e) {
				log.error("视频文件无效或格式不支持: {}", videoPath, e);
				throw new IllegalArgumentException("无效的视频文件或格式不支持: " + videoPath, e);
			}

			int frameRate = (int) initialGrabber.getVideoFrameRate();
			int totalFrames = initialGrabber.getLengthInFrames();
			int frameInterval = frameRate * interval;

			log.info("视频信息 - 帧率: {}, 总帧数: {}, 实际抽帧间隔: {}帧", frameRate, totalFrames, frameInterval);

			if (frameRate <= 0 || totalFrames <= 0) {
				throw new IllegalArgumentException("无效的视频信息：帧率=" + frameRate + ", 总帧数=" + totalFrames);
			}

			Frame firstFrame = initialGrabber.grab(); // 进一步验证
			if (firstFrame == null || firstFrame.image == null) {
				throw new IllegalArgumentException("无法读取视频帧，请检查视频文件是否有效");
			}
			initialGrabber.stop(); // 校验完成，关闭

			Files.createDirectories(Paths.get(outputDir)); // 确保输出目录存在

			List<Integer> allFrameNumbers = new ArrayList<>();
			for (int i = 0; i < totalFrames; i += frameInterval) {
				allFrameNumbers.add(i);
			}

			if (allFrameNumbers.isEmpty()) {
				log.warn("没有需要提取的帧。");
				return new ArrayList<>();
			}

			int numberOfThreads = ((ThreadPoolExecutor) frameExtractorExecutor).getCorePoolSize();
			// 避免除零，至少分1块
			int chunksCount = Math.max(1, numberOfThreads);
			int chunkSize = (int) Math.ceil((double) allFrameNumbers.size() / chunksCount);
			// 确保 chunkSize 至少为 1
			chunkSize = Math.max(1, chunkSize);

			List<List<Integer>> frameNumberChunks = Lists.partition(allFrameNumbers, chunkSize);

			CountDownLatch latch = new CountDownLatch(frameNumberChunks.size());
			List<Future<?>> futures = new ArrayList<>();
			log.info("将 {} 帧分成 {} 个块进行处理，每块约 {} 帧", allFrameNumbers.size(), frameNumberChunks.size(), chunkSize);

			for (List<Integer> chunk : frameNumberChunks) {
				Future<?> future = frameExtractorExecutor.submit(() -> {
					try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
						 Java2DFrameConverter converter = new Java2DFrameConverter()) {
						try {
							grabber.setOption("hwaccel", "cuda"); // 或其他，或移除此行如果不需要
						} catch (Exception e) { /* log */ }
						grabber.start();

						for (int frameNumber : chunk) {
							File frameFile = extractSingleFrameInternal(grabber, converter, frameNumber, outputDir);
							if (frameFile != null) {
								if (batchProcessor != null) {
									// 使用批处理回调 (SSE)
									synchronized (batchLock) {
										currentBatch.add(FileDTO.builder()
												.fileName(frameFile.getAbsolutePath())
												.originalFileName(frameFile.getName())
												.timeStamp(System.currentTimeMillis())
												.build());
										if (currentBatch.size() >= batchSize) {
											// 排序当前批次再发送 (可选但推荐)
											currentBatch.sort(Comparator.comparingInt(f -> Integer.parseInt(f.getOriginalFileName().split("_")[1].split("\\.")[0])));
											batchProcessor.accept(new ArrayList<>(currentBatch)); // 发送副本
											currentBatch.clear();
										}
									}
								} else {
									// 直接收集结果 (异步)
									sortedFrameFiles.put(frameNumber, FileDTO.builder()
											.fileName(frameFile.getAbsolutePath())
											.originalFileName(frameFile.getName())
											.timeStamp(System.currentTimeMillis())
											.build());
								}
							}
						}
					} catch (Exception e) {
						log.error("处理帧块时发生严重错误", e);
						// 考虑是否需要将错误传递出去
					} finally {
						latch.countDown();
					}
				});
				futures.add(future);
			}

			latch.await(); // 等待所有块处理完成

			// 处理剩余的批次 (SSE)
			if (batchProcessor != null) {
				synchronized (batchLock) {
					if (!currentBatch.isEmpty()) {
						currentBatch.sort(Comparator.comparingInt(f -> Integer.parseInt(f.getOriginalFileName().split("_")[1].split("\\.")[0])));
						batchProcessor.accept(new ArrayList<>(currentBatch));
						currentBatch.clear();
					}
				}
			}

			// 检查任务异常
			for (Future<?> f : futures) {
				try {
					f.get();
				} catch (InterruptedException | ExecutionException e) {
					log.error("帧块处理任务执行时发生错误", e);
					// 可以选择向上抛出异常
					// throw new RuntimeException("Frame extraction sub-task failed", e);
				}
			}

			long endTime = System.currentTimeMillis();
			int extractedCount = (batchProcessor == null) ? sortedFrameFiles.size() : -1; // SSE 模式下计数不直接可用
			log.info("核心处理完成，耗时: {} 毫秒. (提取数量: {})", (endTime - startTime),
					(extractedCount != -1) ? extractedCount : "N/A for SSE");

			return (batchProcessor == null) ? new ArrayList<>(sortedFrameFiles.values()) : new ArrayList<>();

		} // 初始 grabber 的 try-with-resources 结束
	}

	// --- 内部单帧提取 (基本不变) ---
	private File extractSingleFrameInternal(FFmpegFrameGrabber grabber, Java2DFrameConverter converter, int frameNumber, String outputDir) {
		try {
			grabber.setFrameNumber(frameNumber);
			Frame frame = grabber.grab();
			if (frame == null || frame.image == null) {
				log.warn("帧 {} 无效或为空", frameNumber);
				return null;
			}
			BufferedImage bufferedImage = converter.convert(frame);
			if (bufferedImage == null || bufferedImage.getWidth() <= 0 || bufferedImage.getHeight() <= 0) {
				log.warn("帧 {} 转换或尺寸无效", frameNumber);
				return null;
			}
			String fileName = String.format("frame_%08d.jpg", frameNumber);
			File outputFile = new File(outputDir, fileName);
			ImageIO.write(bufferedImage, "jpg", outputFile);
			// log.debug("成功提取帧: {} (由块任务处理)", fileName); // 日志可能过多
			return outputFile;
		} catch (Exception e) {
			log.error("提取帧 {} 时发生错误 (在块任务内): {}", frameNumber, e.getMessage());
			return null;
		}
	}

	// --- 文件处理 ---
	private Path saveTemporaryFile(MultipartFile file, String prefix) throws IOException {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("上传的文件不能为空");
		}
		// 可考虑更安全的临时文件目录配置
		Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "video_uploads");
		Files.createDirectories(tempDir);
		// 使用原始文件名可能导致冲突或安全问题，用前缀+原始名或UUID
		String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
		// 清理文件名中的非法字符
		originalFilename = originalFilename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
		Path tempFilePath = tempDir.resolve(prefix + "_" + originalFilename);
		file.transferTo(tempFilePath);
		log.info("文件已临时保存到: {}", tempFilePath);
		return tempFilePath;
	}

	private void cleanupTemporaryFile(String filePath) {
		try {
			log.info("尝试删除临时文件: {}", filePath);
			// Files.deleteIfExists(Paths.get(filePath));
			log.info("临时文件已删除: {}", filePath);
		} catch (Exception e) {
			log.error("删除临时文件失败: {}", filePath, e);
		}
	}

	/**
	 * @param resultFilePaths 存储结果文件路径
	 */ // --- 辅助类 ---
	public record AsyncTaskStatus(String taskId, AsyncTaskState state, String message, List<String> resultFilePaths) {
	}

	public enum AsyncTaskState {
		PENDING, RUNNING, COMPLETED, FAILED
	}

	// 服务销毁时关闭线程池
	@PreDestroy
	public void shutdown() {
		log.info("Shutting down VideoService frameExtractorExecutor...");
		frameExtractorExecutor.shutdown();
		try {
			if (!frameExtractorExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
				frameExtractorExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			frameExtractorExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
		log.info("VideoService frameExtractorExecutor shut down.");
	}
}
