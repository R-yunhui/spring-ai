package com.ral.young.spring.ai.model.enums;

/**
 * 内容块类型枚举。
 * 用于定义文档中不同类型的内容单元，如段落、表格或图片。
 */
public enum BlockType {
    /**
     * 段落类型。
     * 代表一段文本。
     */
    PARAGRAPH,

    /**
     * 表格类型。
     * 代表一个结构化的数据表格。
     */
    TABLE,

    /**
     * 图片类型。
     * 代表一张嵌入的图像。
     */
    IMAGE
} 