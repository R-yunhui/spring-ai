package com.ral.young.spring.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author renyh
 * @description 文件传输对象
 * @date 2025/4/21 10:11
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileDTO {

	/**
	 * 文件名称（后续可能是上传到 minio 或者 gofast 的地址）
	 */
	private String fileName;

	/**
	 * 原始文件名称
	 */
	private String originalFileName;

	/**
	 * 时间戳
	 */
	private Long timeStamp;
}
