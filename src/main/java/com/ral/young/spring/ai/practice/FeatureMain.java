package com.ral.young.spring.ai.practice;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author renyh
 * @description jdk21新特性学习
 * @date 2025/5/27 14:00
 * @since 1.0.0
 */
@Slf4j
@SuppressWarnings("preview")
public class FeatureMain {

	public static void main(String[] args) throws InterruptedException, JsonProcessingException {
		testRecord();

		// testVar();

		// testInstanceOf(Map.of("circle", new Circle(10)));

		// testThread();
	}

	private static void testThread() throws InterruptedException {
		/*
		 * 通过耗时对比传统线程和虚拟线程
		 * 1. 虚拟线程：虚拟线程是JDK21引入的，它与普通线程类似，但运行速度更快，并且更轻量级。
		 * 2. 虚拟线程适用 I/O密集型任务，如网络请求、文件读写等。
		 * 3. 虚拟现场线程创建数量几乎无限制
		 */
		System.out.println("----- 虚拟线程 - 传统线程 -----");
		CompletableFuture.runAsync(FeatureMain::testTraditionalThread);
		CompletableFuture.runAsync(FeatureMain::testVirtualThread);

		Thread.sleep(3000);
		log.info("主线程结束");
	}

	private static void testVar() {
		/*
		 * var 关键字
		 * 1. 是一个关键字，用于声明一个局部变量
		 * 2. 编译的时候自动推断变量的类型
		 * 3. 只能用于局部变量
		 */
		System.out.println("----- var -----");
		var name = "李斯";
		var userList = List.of(new User("张三", 18), new User("李四", 19));
		var age = 21;
		System.out.println(name);
		System.out.println(userList);
		System.out.println(age);
	}

	private static void testRecord() throws JsonProcessingException {
		System.out.println("----- record -----");
		User user = new User("张三", 18);
		String jsonStr = JSONUtil.toJsonStr(user);
		System.out.println(STR."HUTOOL JSON: \{jsonStr}");
		ObjectMapper objectMapper = new ObjectMapper();
		String valueAsString = objectMapper.writeValueAsString(user);
		System.out.println(STR."OBJECT MAPPER: \{valueAsString}");
		System.out.println(user);
		user.sayAge();
	}

	private static void testInstanceOf(Map<String, Object> map) {
		System.out.println("----- instanceof -----");
		var o = map.get("circle");

		// Java 21 完整模式匹配
		// instanceof 表达式可以匹配任意类型，包括基本类型和自定义类型
		if (o instanceof Circle c) {
			System.out.println(STR."圆形，半径: \{c.radius()}");
		}

		// Switch表达式配合模式匹配
		String desc = switch (o) {
			case Circle(var r) -> STR."圆形, 半径 \{r}";
			case Rectangle(var w, var h) -> STR."矩形 \{w}x\{h}";
			case null -> "空形状";
			default -> "未知形状";
		};

		System.out.println(desc);
	}

	interface Shape {
	}

	record Circle(double radius) implements Shape {
	}

	record Rectangle(double w, double h) implements Shape {
	}

	public static void testVirtualThread() {
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			long start = System.currentTimeMillis();

			// 使用虚拟线程处理1000个任务
			for (int i = 0; i < 1000; i++) {
				final int taskId = i;
				executor.submit(() -> {
					log.info("开始处理任务 {}, 虚拟线程: {}", taskId, Thread.currentThread().getName());
					try {
						Thread.sleep(100); // 模拟I/O等待
					} catch (InterruptedException e) {
						log.error("线程被中断", e);
					}
					log.info("虚拟现场完成任务 {}", taskId);
				});
			}

			// 等待所有虚拟线程完成
			executor.shutdown();
			try {
				// 等待所有任务完成，最多等待指定时间
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					// 如果超时仍有任务未完成
					executor.shutdownNow(); // 取消正在执行的任务
					if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
						log.error("虚拟线程池未能终止");
					}
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt(); // 恢复中断状态
			}

			long duration = System.currentTimeMillis() - start;
			log.info("虚拟线程池方式总耗时，总耗时：{} 毫秒", duration);
		}
	}

	public static void testTraditionalThread() {
		// 创建固定大小的线程池(100个线程)
		try (var executor = new ThreadPoolExecutor(100, 100, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(10000),
				ThreadFactoryBuilder.create().setNamePrefix("traditional-thread-pool-").build(),
				new ThreadPoolExecutor.DiscardPolicy()
		);) {
			long start = System.currentTimeMillis();

			// 提交1000个任务
			for (int i = 0; i < 1000; i++) {
				final int taskId = i;
				executor.submit(() -> {
					log.info("开始执行任务 {}, 传统线程: {}", taskId, Thread.currentThread().getName());
					try {
						Thread.sleep(100); // 模拟I/O等待
					} catch (InterruptedException e) {
						log.error("线程被中断", e);
					}
					log.info("完成任务 {}", taskId);
				});
			}

			executor.shutdown();
			try {
				// 等待所有任务完成，最多等待指定时间
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					// 如果超时仍有任务未完成
					executor.shutdownNow(); // 取消正在执行的任务
					if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
						log.error("传统虚拟线程池未能终止");
					}
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt(); // 恢复中断状态
			}

			long duration = System.currentTimeMillis() - start;
			log.info("传统线程池方式总耗时，总耗时：{} 毫秒", duration);
		} catch (Exception e) {
			log.error("传统线程池执行异常", e);
		}
	}

	public record User(@JsonProperty(value = "name") String name, @JsonProperty(value = "age") int age) {
		/*
		 * record关键字
		 * 1. 是一个关键字，用于定义一个不可变的数据类
		 * 2. 可以自动生成构造方法、getter方法、equals方法、hashCode方法、toString方法
		 *
		 * 适用场景：
		 * 1. 创建对象时，对象属性不可变，且属性数量少时使用（当类仅用于存储数据（没有复杂行为）时，record 可以替代传统 POJO。）
		 * 2. 不可变配置或参数（如果类的字段在创建后不允许修改，record 的默认不可变性非常合适。）
		 * 3.值对象（当对象的相等性由字段值决定时（如日期、金额、坐标），record 自动生成的 equals() 和 hashCode() 非常有用。）
		 * 4.模式匹配的配合使用（record 与 switch 模式匹配（JDK 21+）结合时，代码更简洁。）
		 *
		 */

		public void sayAge() {
			System.out.println(STR."年龄是：\{age}");
		}
	}
}
