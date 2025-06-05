package com.ral.young.spring.ai.practice;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;

/**
 * @author renyh
 * @description 结构化并发编程练习 - 使用 JDK 21 新特性
 * @date 2025/6/4 16:10
 * @since 1.0.0
 */
@Slf4j
@SuppressWarnings("preview")
public class StructuredConcurrencyProgram {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	public static void main(String[] args) {
		try {
			StructuredConcurrencyProgram program = new StructuredConcurrencyProgram();
			UserDashboard dashboard = program.getUserDashboardData(IdUtil.getSnowflakeNextId());
			log.info("成功获取用户仪表盘数据: {}", dashboard);
		} catch (Exception e) {
			log.error("程序执行失败", e);
		}
	}

	/**
	 * 获取用户仪表盘数据
	 * 使用结构化并发同时获取用户信息、订单列表和推荐列表
	 *
	 * @param userId 用户ID
	 * @return 用户仪表盘数据
	 */
	public UserDashboard getUserDashboardData(Long userId) throws TimeoutException {
		// 创建虚拟线程工厂，使用命名模式便于调试
		ThreadFactory virtualThreadFactory = Thread.ofVirtual()
				.name("virtual-thread-", 0)
				.factory();

		// 使用 ShutdownOnFailure 策略创建结构化任务作用域
		try (var scope = new StructuredTaskScope.ShutdownOnFailure("UserDashboardScope", virtualThreadFactory)) {
			log.info("开始获取用户仪表盘数据, userId: {}", userId);

			// 并行执行三个子任务
			var userInfoTask = scope.fork(() -> getUserInfo(userId));
			var ordersTask = scope.fork(() -> getOrderList(userId));
			var recommendationsTask = scope.fork(() -> getRecommendationList(userId));

			// 设置超时时间
			scope.joinUntil(Instant.now().plus(TIMEOUT));

			// 检查是否有任务失败
			scope.throwIfFailed(e -> {
				log.error("获取用户仪表盘数据时发生错误", e);
				return new RuntimeException("获取用户仪表盘数据失败", e);
			});

			// 获取所有任务的结果
			UserInfo userInfo = userInfoTask.get();
			List<Order> orders = ordersTask.get();
			List<Recommendation> recommendations = recommendationsTask.get();

			log.info("成功获取所有数据: 用户信息={}, 订单数={}, 推荐数={}",
					userInfo, orders.size(), recommendations.size());

			return new UserDashboard(userInfo, orders, recommendations);
		} catch (TimeoutException e) {
			log.error("获取用户仪表盘数据超时", e);
			throw e;
		} catch (Exception e) {
			log.error("获取用户仪表盘数据失败", e);
			throw new RuntimeException("获取用户仪表盘数据失败", e);
		}
	}

	/**
	 * 获取用户基本信息
	 */
	private UserInfo getUserInfo(Long userId) throws InterruptedException {
		log.info("开始获取用户信息, userId: {}", userId);
		try {
			// 模拟网络请求延迟
			Thread.sleep(Duration.ofMillis(100));
			return new UserInfo("RenYH", "RenYH@gmail.com");
		} catch (InterruptedException e) {
			log.error("获取用户信息被中断", e);
			Thread.currentThread().interrupt();
			throw e;
		} finally {
			log.info("完成获取用户信息, userId: {}", userId);
		}
	}

	/**
	 * 获取用户订单列表
	 */
	private List<Order> getOrderList(Long userId) throws InterruptedException {
		log.info("开始获取用户订单列表, userId: {}", userId);
		try {
			// 模拟网络请求延迟
			Thread.sleep(Duration.ofMillis(500));
			return List.of(
					new Order(1L, "Order1", BigDecimal.valueOf(100)),
					new Order(2L, "Order2", BigDecimal.valueOf(200))
			);
		} catch (InterruptedException e) {
			log.error("获取订单列表被中断", e);
			Thread.currentThread().interrupt();
			throw e;
		} finally {
			log.info("完成获取用户订单列表, userId: {}", userId);
		}
	}

	/**
	 * 获取用户推荐列表
	 */
	private List<Recommendation> getRecommendationList(Long userId) throws InterruptedException {
		log.info("开始获取用户推荐列表, userId: {}", userId);
		try {
			// 模拟网络请求延迟
			Thread.sleep(Duration.ofMillis(200));
			return List.of(
					new Recommendation(1L, "Product1", BigDecimal.valueOf(50)),
					new Recommendation(2L, "Product2", BigDecimal.valueOf(75))
			);
		} catch (InterruptedException e) {
			log.error("获取推荐列表被中断", e);
			Thread.currentThread().interrupt();
			throw e;
		} finally {
			log.info("完成获取用户推荐列表, userId: {}", userId);
		}
	}

	// 内部数据类，使用 private 修饰符
	private record UserInfo(String name, String email) {}

	private record Order(Long orderId, String orderName, BigDecimal orderPrice) {}

	private record Recommendation(Long productId, String productName, BigDecimal productPrice) {}

	// 将 UserDashboard 改为内部类
	public record UserDashboard(UserInfo userInfo, List<Order> recentOrders, List<Recommendation> recommendations) {}
}