package com.ral.young.spring.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 标题实体。
 * 用于表示文档中的标题元素（如H1、H2等）。
 * 包含标题的级别、文本内容和唯一标识符。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Heading {
    /**
     * 标题级别。
     * 通常是一个整数，例如 1 对应 H1，2 对应 H2，以此类推。
     * Word 中的标题样式（如"标题 1"、"标题 2"）需要映射到这些级别。
     */
    private int level;

    /**
     * 标题的纯文本内容。
     */
    private String text;

    /**
     * 标题的唯一标识符。
     * 根据设计文档，章ID (chapterId) 为 "ch_" + H1标题哈希。
     * 节ID (sectionId) 可能类似，例如 "sec_" + H2标题哈希。
     * 此ID用于在文档结构中唯一标识一个标题，并作为 {@link com.ral.young.spring.ai.model.block.Block#parentHeadingId} 的引用。
     */
    private String id;
} 