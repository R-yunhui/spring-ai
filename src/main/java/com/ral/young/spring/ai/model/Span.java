package com.ral.young.spring.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 带格式的文本片段实体。
 * 代表段落内具有特定样式（如加粗、颜色、字体等）的文本部分。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Span {
    /**
     * 文本片段的实际内容。
     */
    private String text;

    /**
     * 格式信息。
     * 以键值对形式存储样式属性，例如: {"bold": true, "color": "FF0000", "fontFamily": "Calibri"}。
     * 具体的格式属性键名和值可以根据实际从 Word 文档中解析到的信息定义。
     */
    private Map<String, Object> style;
} 