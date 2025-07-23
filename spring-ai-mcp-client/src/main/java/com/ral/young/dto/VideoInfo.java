package com.ral.young.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author renyh
 * @description 视频基础信息
 * @date 2025/7/23 10:44
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoInfo implements Serializable {

	/**
	 * 视频唯一标识
	 */
	private String videoId;

	/**
	 * 标题
	 */
	private String title;

	/**
	 * 描述
	 */
	private String description;

	/**
	 * 开始时间
	 */
	private String startTime;

	/**
	 * 结束时间
	 */
	private String endTime;

	/**
	 * 时长(秒)
	 */
	private Integer duration;

	/**
	 * 标签列表(如"人员入侵"、"黑色上衣"等)
	 */
	private List<String> tags;
}
