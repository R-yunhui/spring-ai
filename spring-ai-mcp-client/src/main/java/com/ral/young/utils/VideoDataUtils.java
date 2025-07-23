package com.ral.young.utils;

import com.ral.young.dto.VideoInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author renyh
 * @description 视频数据工具类，提供共享的数据生成和工具方法
 * @date 2025/7/23 10:44
 * @since 1.0.0
 */
@SuppressWarnings("preview")
public class VideoDataUtils {

	// 视频缓存，模拟数据库
	private static final ConcurrentHashMap<String, VideoInfo> VIDEO_CACHE = new ConcurrentHashMap<>();

	// 日期时间格式化器
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	// 初始化模拟数据
	static {
		initMockData();
	}

	/**
	 * 初始化模拟数据
	 */
	private static void initMockData() {
		// 1. 人员入侵事件视频
		createPersonIntrusionVideos();

		// 2. 动物入侵事件视频
		createAnimalIntrusionVideos();

		// 3. 穿黑色上衣的男人视频
		createBlackShirtManVideos();

		// 4. 交通违规行为视频
		createTrafficViolationVideos();
	}

	/**
	 * 创建人员入侵事件视频
	 */
	private static void createPersonIntrusionVideos() {
		// 视频1: 仓库后门入侵
		VideoInfo video1 = VideoInfo.builder()
				.videoId("PI_001")
				.title("仓库后门入侵事件")
				.description("仓库后门区域，一名戴口罩的陌生男子尝试撬门进入，监控清晰记录了整个过程")
				.startTime(LocalDateTime.now().minusDays(2).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(2).plusMinutes(5).format(DATE_TIME_FORMATTER))
				.duration(300)
				.tags(Arrays.asList("人员入侵", "仓库", "撬门", "口罩男子", "安全事件"))
				.build();
		VIDEO_CACHE.put(video1.getVideoId(), video1);

		// 视频2: 办公区非工作时间入侵
		VideoInfo video2 = VideoInfo.builder()
				.videoId("PI_002")
				.title("办公区非工作时间入侵")
				.description("办公区走廊，非工作时间有陌生人员活动，身穿深色外套，尝试打开多个办公室门")
				.startTime(LocalDateTime.now().minusDays(3).withHour(23).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(3).withHour(23).plusMinutes(8).format(DATE_TIME_FORMATTER))
				.duration(480)
				.tags(Arrays.asList("人员入侵", "办公区", "非工作时间", "陌生人", "安全事件"))
				.build();
		VIDEO_CACHE.put(video2.getVideoId(), video2);

		// 视频3: 围墙翻越入侵
		VideoInfo video3 = VideoInfo.builder()
				.videoId("PI_003")
				.title("围墙翻越入侵事件")
				.description("工厂围墙外，两名男子尝试攀爬围墙进入厂区，其中一人成功翻越，另一人失败")
				.startTime(LocalDateTime.now().minusDays(1).withHour(2).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(1).withHour(2).plusMinutes(6).format(DATE_TIME_FORMATTER))
				.duration(360)
				.tags(Arrays.asList("人员入侵", "围墙翻越", "工厂", "多人", "安全事件"))
				.build();
		VIDEO_CACHE.put(video3.getVideoId(), video3);
	}

	/**
	 * 创建动物入侵事件视频
	 */
	private static void createAnimalIntrusionVideos() {
		// 视频1: 野狗入侵仓库
		VideoInfo video1 = VideoInfo.builder()
				.videoId("AI_001")
				.title("野狗入侵仓库事件")
				.description("仓库后门，一只野狗通过未关闭的小门进入仓库内部，在货架间游荡")
				.startTime(LocalDateTime.now().minusDays(4).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(4).plusMinutes(7).format(DATE_TIME_FORMATTER))
				.duration(420)
				.tags(Arrays.asList("动物入侵", "野狗", "仓库", "未关门"))
				.build();
		VIDEO_CACHE.put(video1.getVideoId(), video1);

		// 视频2: 猫科动物入侵办公区
		VideoInfo video2 = VideoInfo.builder()
				.videoId("AI_002")
				.title("猫科动物入侵办公区")
				.description("办公区窗户，一只野猫通过打开的窗户进入办公区，在桌面和设备上行走")
				.startTime(LocalDateTime.now().minusDays(5).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(5).plusMinutes(10).format(DATE_TIME_FORMATTER))
				.duration(600)
				.tags(Arrays.asList("动物入侵", "野猫", "办公区", "开窗"))
				.build();
		VIDEO_CACHE.put(video2.getVideoId(), video2);

		// 视频3: 鸟类入侵配电室
		VideoInfo video3 = VideoInfo.builder()
				.videoId("AI_003")
				.title("鸟类入侵配电室")
				.description("配电室通风口，多只鸟类通过通风口进入配电室，在设备上筑巢")
				.startTime(LocalDateTime.now().minusDays(6).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(6).plusMinutes(8).format(DATE_TIME_FORMATTER))
				.duration(480)
				.tags(Arrays.asList("动物入侵", "鸟类", "配电室", "筑巢", "安全隐患"))
				.build();
		VIDEO_CACHE.put(video3.getVideoId(), video3);
	}

	/**
	 * 创建穿黑色上衣的男人视频
	 */
	private static void createBlackShirtManVideos() {
		// 视频1: 商场监控中的黑衣男子
		VideoInfo video1 = VideoInfo.builder()
				.videoId("BM_001")
				.title("商场监控中的黑衣男子")
				.description("商场一楼电梯附近，一名穿黑色夹克的男子形迹可疑，多次往员工通道张望")
				.startTime(LocalDateTime.now().minusDays(2).withHour(14).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(2).withHour(14).plusMinutes(5).format(DATE_TIME_FORMATTER))
				.duration(300)
				.tags(Arrays.asList("黑色上衣", "男性", "商场", "可疑行为", "电梯附近"))
				.build();
		VIDEO_CACHE.put(video1.getVideoId(), video1);

		// 视频2: 停车场的黑衣男子
		VideoInfo video2 = VideoInfo.builder()
				.videoId("BM_002")
				.title("停车场的黑衣男子")
				.description("地下停车场B2层，一名身穿黑色T恤和深色牛仔裤的男性在车辆间徘徊")
				.startTime(LocalDateTime.now().minusDays(3).withHour(20).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(3).withHour(20).plusMinutes(6).format(DATE_TIME_FORMATTER))
				.duration(360)
				.tags(Arrays.asList("黑色上衣", "黑色T恤", "男性", "停车场", "徘徊"))
				.build();
		VIDEO_CACHE.put(video2.getVideoId(), video2);

		// 视频3: 咖啡厅的黑衣男子
		VideoInfo video3 = VideoInfo.builder()
				.videoId("BM_003")
				.title("咖啡厅的黑衣男子")
				.description("咖啡厅靠窗座位，一名穿黑色衬衫的男性顾客正在使用笔记本电脑，时常观察门口")
				.startTime(LocalDateTime.now().minusDays(1).withHour(16).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(1).withHour(16).plusMinutes(45).format(DATE_TIME_FORMATTER))
				.duration(2700)
				.tags(Arrays.asList("黑色上衣", "黑色衬衫", "男性", "咖啡厅", "笔记本电脑"))
				.build();
		VIDEO_CACHE.put(video3.getVideoId(), video3);
	}

	/**
	 * 创建交通违规行为视频
	 */
	private static void createTrafficViolationVideos() {
		// 视频1: 闯红灯事件
		VideoInfo video1 = VideoInfo.builder()
				.videoId("TV_001")
				.title("东西路口闯红灯事件")
				.description("东西主干道十字路口，一辆白色轿车在红灯亮起后仍然通过路口")
				.startTime(LocalDateTime.now().minusDays(2).withHour(8).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(2).withHour(8).plusMinutes(1).format(DATE_TIME_FORMATTER))
				.duration(60)
				.tags(Arrays.asList("交通违规", "闯红灯", "白色轿车", "十字路口"))
				.build();
		VIDEO_CACHE.put(video1.getVideoId(), video1);

		// 视频2: 逆行事件
		VideoInfo video2 = VideoInfo.builder()
				.videoId("TV_002")
				.title("单行道逆行事件")
				.description("城南单行道，一辆黑色摩托车逆向行驶，造成其他车辆紧急避让")
				.startTime(LocalDateTime.now().minusDays(3).withHour(17).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(3).withHour(17).plusMinutes(2).format(DATE_TIME_FORMATTER))
				.duration(120)
				.tags(Arrays.asList("交通违规", "逆行", "摩托车", "单行道", "危险驾驶"))
				.build();
		VIDEO_CACHE.put(video2.getVideoId(), video2);

		// 视频3: 占用应急车道
		VideoInfo video3 = VideoInfo.builder()
				.videoId("TV_003")
				.title("高速公路占用应急车道")
				.description("城市环线高速公路，一辆蓝色货车长时间占用应急车道行驶")
				.startTime(LocalDateTime.now().minusDays(1).withHour(10).format(DATE_TIME_FORMATTER))
				.endTime(LocalDateTime.now().minusDays(1).withHour(10).plusMinutes(3).format(DATE_TIME_FORMATTER))
				.duration(180)
				.tags(Arrays.asList("交通违规", "占用应急车道", "货车", "高速公路"))
				.build();
		VIDEO_CACHE.put(video3.getVideoId(), video3);
	}

	/**
	 * 根据ID获取视频信息
	 */
	public static VideoInfo getVideoById(String videoId) {
		return VIDEO_CACHE.get(videoId);
	}

	/**
	 * 根据标签搜索视频
	 */
	public static List<VideoInfo> searchVideosByTags(List<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return new ArrayList<>();
		}

		return VIDEO_CACHE.values().stream()
				.filter(video -> {
					List<String> videoTags = video.getTags();
					return videoTags != null && videoTags.stream().anyMatch(tags::contains);
				})
				.collect(Collectors.toList());
	}

	/**
	 * 根据关键词搜索视频(标题和描述)
	 */
	public static List<VideoInfo> searchVideosByKeywords(List<String> keywords) {
		if (keywords == null || keywords.isEmpty()) {
			return new ArrayList<>();
		}

		return VIDEO_CACHE.values().stream()
				.filter(video -> {
					String titleAndDesc = (STR."\{video.getTitle()} \{video.getDescription()}").toLowerCase();
					return keywords.stream()
							.anyMatch(keyword -> titleAndDesc.contains(keyword.toLowerCase()));
				})
				.collect(Collectors.toList());
	}

	/**
	 * 获取所有视频
	 */
	public static List<VideoInfo> getAllVideos() {
		return new ArrayList<>(VIDEO_CACHE.values());
	}

	/**
	 * 根据视频ID列表获取视频信息
	 */
	public static List<VideoInfo> getVideosByIds(List<String> videoIds) {
		if (videoIds == null || videoIds.isEmpty()) {
			return new ArrayList<>();
		}

		return videoIds.stream()
				.map(VIDEO_CACHE::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}