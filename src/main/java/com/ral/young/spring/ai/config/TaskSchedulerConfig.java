package com.ral.young.spring.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author renyh
 * @description 任务调度器配置
 * @date 2025/6/11 17:05
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableScheduling
public class TaskSchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        log.info("开始创建 TaskScheduler Bean");
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 设置线程池大小
        scheduler.setPoolSize(5);
        // 设置线程名前缀
        scheduler.setThreadNamePrefix("task-scheduler-");
        // 设置等待所有任务完成后再关闭线程池
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        // 设置等待时间
        scheduler.setAwaitTerminationSeconds(60);
        // 初始化线程池
        scheduler.initialize();
        log.info("TaskScheduler Bean 创建完成");
        return scheduler;
    }
} 