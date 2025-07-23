package com.ral.young.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author renyh
 * @description 算法匹配响应
 * @date 2025/7/25 10:32
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmMatchingResponse implements Serializable {

	/**
	 * 是否匹配成功
	 */
	private boolean matched;

	/**
	 * 匹配到的算法类型
	 */
	private String algorithmType;

	/**
	 * 算法描述
	 */
	private String algorithmDescription;

	/**
	 * 视频ID列表
	 */
	private List<String> videoIds;

	/**
	 * 下一步工具
	 */
	private String nextTool;

	/**
	 * 匹配结果说明
	 */
	private String matchResult;
}