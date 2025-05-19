package com.ral.young.spring.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 文档主实体。
 * 代表整个被解析的Word文档的顶层结构。
 * 包含文档的唯一标识、主标题、章 ({@link Chapter}) 列表以及元数据 ({@link DocumentMetadata})。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    /**
     * 文档的唯一标识符。
     * 根据设计文档，是文档内容的MD5哈希值。也可以是文件名或其他唯一识别码。
     */
    private String docId;

    /**
     * 文档的主标题。
     * 这可能是从文件名、文档属性中提取的标题，或者文档中的第一个H1标题（如果适用）。
     * 具体来源取决于解析策略。
     */
    private String title;

    /**
     * 文档包含的章列表。
     * 每个 {@link Chapter} 对象代表文档中的一个主要部分，通常由H1标题开始。
     * 根据设计文档，一个文档至少包含一个Chapter。
     * @see Chapter
     */
    private List<Chapter> chapters;

    /**
     * 文档的元数据信息。
     * 包含作者、创建日期等从文档属性中提取的信息。
     * @see DocumentMetadata
     */
    private DocumentMetadata metadata;

    // 根据您的需求，可以考虑添加一个未分类内容块的列表，用于存放无法归入任何章节的元素。
    // private List<com.ral.young.spring.ai.model.block.Block> unclassifiedBlocks;
} 