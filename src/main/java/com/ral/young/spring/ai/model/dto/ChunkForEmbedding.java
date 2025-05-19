package com.ral.young.spring.ai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 用于封装待Embedding的文本片段及其元数据。
 * <p>
 * 这个类代表了从原始文档中提取出来，并经过处理后，
 * 准备送入Embedding模型进行向量化的一条数据。
 * </p>
 */
@Data // Lombok: 自动生成 getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: 自动生成无参构造函数
@AllArgsConstructor // Lombok: 自动生成全参构造函数
public class ChunkForEmbedding {

	/**
	 * 文本片段的唯一ID。
	 * 通常由文档ID、原始块ID和块内片段索引组合而成，
	 * 例如: "docId_blockId_chunk_001"。
	 */
	private String chunkId;

	/**
	 * 该片段所属的原始文档的ID。
	 * 用于关联回原始文档。
	 */
	private String documentId;

	/**
	 * 原始文档的文件名。
	 * 便于追溯和展示。
	 */
	private String originalFilename;

	/**
	 * 实际用于生成Embedding的文本内容。
	 * 这部分文本通常会包含从文档结构中提取的上下文信息，
	 * 如章节标题、节标题等，以增强Embedding的语义表达能力。
	 */
	private String chunkText;

	/**
	 * 一个Map结构，用于存储与该文本片段相关的详细元数据。
	 * 这些元数据对于后续的检索过滤、结果排序、上下文构建以及
	 * 向用户展示信息来源都非常重要。
	 * <p>
	 * 示例元数据键值对:
	 * <ul>
	 *   <li>"chapter_id": "ch_abc" (所属章节ID)</li>
	 *   <li>"chapter_title": "第一章 引言" (所属章节标题)</li>
	 *   <li>"section_id": "sec_def" (所属节ID)</li>
	 *   <li>"section_title": "1.1 背景" (所属节标题)</li>
	 *   <li>"block_id": "para_ghi" (原始块ID)</li>
	 *   <li>"block_type": "PARAGRAPH" (原始块类型)</li>
	 *   <li>"order_in_parent": 0 (在父结构中的顺序)</li>
	 *   <li>"original_content_preview": "这是段落的开头..." (原始块内容的预览)</li>
	 *   <li>"image_filename": "figure1.png" (如果是图片块)</li>
	 *   <li>"table_row_count": 10 (如果是表格块)</li>
	 * </ul>
	 * </p>
	 */
	private Map<String, Object> metadata;
}