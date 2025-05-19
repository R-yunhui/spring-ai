package com.ral.young.spring.ai.model.block;

import com.ral.young.spring.ai.model.Span;
import com.ral.young.spring.ai.model.Position;
import com.ral.young.spring.ai.model.enums.BlockType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 段落实体。
 * 表示文档中的一个文本段落，继承自 {@link Block}。
 * 包含纯文本内容以及一个 {@link Span} 对象列表，用于表示带格式的文本片段。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Paragraph extends Block {
    /**
     * 段落的纯文本内容。
     * 这是段落中所有文本（包括 {@link #spans} 中的文本）的简单串联，不含格式信息。
     * 主要用于快速文本提取或不需要格式的场景。
     */
    private String text;

    /**
     * 带格式的文本片段列表。
     * 每个 {@link Span} 对象代表段落中具有特定样式的一部分文本。
     * 如果段落没有特殊格式，此列表可能只包含一个代表整个段落文本的 Span，或者为空（此时 {@link #text} 存储内容）。
     * @see Span
     */
    private List<Span> spans;

    /**
     * 构造一个新的段落实例。
     * @param blockId 段落的唯一标识符。
     * @param text 段落的纯文本内容。
     * @param spans 段落中带格式的文本片段列表。
     * @param position 段落的位置信息（可选）。
     */
    public Paragraph(String blockId, String text, List<Span> spans, Position position) {
        this.setBlockId(blockId);
        this.setType(BlockType.PARAGRAPH);
        this.text = text;
        this.spans = spans;
        this.setPosition(position);
    }
} 