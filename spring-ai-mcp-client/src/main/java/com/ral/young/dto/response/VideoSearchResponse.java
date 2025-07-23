package com.ral.young.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


/**
 * @author renyh
 * @description 视频搜索响应
 * @date 2025/7/23 10:39
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSearchResponse implements Serializable {

	/**
	 * 检索到的视频ID列表
	 */
	private List<String> videoIds;

	/**
	 * 总数量
	 */
	private Integer totalCount;

	/**
	 * 检索耗时(毫秒)
	 */
	private Long searchTimeMs;

	/**
	 * 建议的下一个工具调用
	 */
	private String nextTool;
}
