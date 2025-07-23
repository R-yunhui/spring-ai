package com.ral.young.manager;

import com.ral.young.enums.TaskStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author renyh
 * @description 异步任务管理器
 * @date 2025/7/23 11:30
 * @since 1.0.0
 */
@Slf4j
@Component
public class TaskManager {

	// 任务状态缓存
	private final ConcurrentHashMap<Long, TaskStatusEnum> taskStatusMap = new ConcurrentHashMap<>();

	// 任务结果缓存
	private final ConcurrentHashMap<Long, Object> taskResultMap = new ConcurrentHashMap<>();

	// 任务计数器，用于生成唯一的任务ID
	private long taskCounter = 1L;

	// 线程池，用于执行异步任务
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

	/**
	 * 提交异步任务
	 *
	 * @param task 任务
	 * @return 任务ID
	 */
	public synchronized Long submitTask(Runnable task) {
		Long taskId = taskCounter++;
		taskStatusMap.put(taskId, TaskStatusEnum.PENDING);

		executor.schedule(() -> {
			try {
				taskStatusMap.put(taskId, TaskStatusEnum.RUNNING);
				task.run();
				taskStatusMap.put(taskId, TaskStatusEnum.COMPLETED);
			} catch (Exception e) {
				log.error("任务执行失败", e);
				taskStatusMap.put(taskId, TaskStatusEnum.FAILED);
				taskResultMap.put(taskId, e.getMessage());
			}
		}, 100, TimeUnit.MILLISECONDS);

		return taskId;
	}

	/**
	 * 提交异步任务并返回结果
	 *
	 * @param task 任务
	 * @return 任务ID
	 */
	public synchronized <T> Long submitTaskWithResult(Supplier<T> task) {
		Long taskId = taskCounter++;
		taskStatusMap.put(taskId, TaskStatusEnum.PENDING);

		executor.schedule(() -> {
			try {
				taskStatusMap.put(taskId, TaskStatusEnum.RUNNING);
				T result = task.get();
				taskResultMap.put(taskId, result);
				taskStatusMap.put(taskId, TaskStatusEnum.COMPLETED);
			} catch (Exception e) {
				log.error("任务执行失败", e);
				taskStatusMap.put(taskId, TaskStatusEnum.FAILED);
				taskResultMap.put(taskId, e.getMessage());
			}
		}, 100, TimeUnit.MILLISECONDS);

		return taskId;
	}

	/**
	 * 获取任务状态
	 *
	 * @param taskId 任务ID
	 * @return 任务状态
	 */
	public TaskStatusEnum getTaskStatus(Long taskId) {
		return taskStatusMap.getOrDefault(taskId, TaskStatusEnum.NOT_FOUND);
	}

	/**
	 * 获取任务结果
	 *
	 * @param taskId 任务ID
	 * @return 任务结果
	 */
	public Object getTaskResult(Long taskId) {
		return taskResultMap.get(taskId);
	}

	/**
	 * 创建任务结果ID
	 *
	 * @param result 结果
	 * @return 结果ID
	 */
	public synchronized Long createResultId(Object result) {
		Long resultId = taskCounter++;
		taskResultMap.put(resultId, result);
		return resultId;
	}

	/**
	 * 获取结果
	 *
	 * @param resultId 结果ID
	 * @return 结果
	 */
	public Object getResult(Long resultId) {
		return taskResultMap.get(resultId);
	}
}