package com.ral.young.spring.ai.model.block;

import com.ral.young.spring.ai.model.Position;
import com.ral.young.spring.ai.model.enums.BlockType;
import lombok.Data;

/**
 * 内容块抽象基类。
 * 文档中结构化内容的基本单元，例如段落 ({@link Paragraph})、表格 ({@link Table}) 或图片 ({@link Image})。
 * 每个块都有一个唯一的ID、类型 ({@link BlockType})、位置信息 ({@link Position})、
 * 指向其所属父级标题的ID，以及其在父容器中的顺序。
 */
@Data
public abstract class Block {
    /**
     * 内容块唯一标识。
     * 通常由解析器在处理时动态生成 (例如使用 UUID 或基于内容的哈希)，以确保其在文档内的唯一性。
     */
    private String blockId;

    /**
     * 内容块类型。
     * 指示该块是段落、表格还是其他类型。定义见 {@link BlockType}。
     */
    private BlockType type;

    /**
     * 内容块在文档中的位置信息。
     * 可能包括页码和坐标。精确获取这些信息对于 Apache POI 可能是复杂的。
     * @see Position
     */
    private Position position;

    /**
     * 内容块所属的上一级标题ID (通常是 {@link com.ral.young.spring.ai.model.Chapter#chapterId} 或 {@link com.ral.young.spring.ai.model.Section#sectionId})。
     * 用于建立内容与文档层级结构的关联。
     */
    private String parentHeadingId;

    /**
     * 内容块在其直接父容器（例如 {@link com.ral.young.spring.ai.model.Section#blocks} 列表）中的顺序索引，从0开始。
     * 用于维持原始内容的出现顺序。
     */
    private int orderInParent;
} 