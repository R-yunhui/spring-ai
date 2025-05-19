package com.ral.young.spring.ai.model;

import com.ral.young.spring.ai.model.block.Block;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 节实体。
 * 通常对应文档中的二级标题 (H2) 及其下的内容。
 * 根据设计文档，H3-H6 级别的标题下的内容也归属于最近的H2节。
 * 一个节包含一个主标题 ({@link Heading}) 和一个内容块 ({@link Block}) 列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Section {
    /**
     * 节的唯一标识符。
     * 例如，可以根据其H2标题的文本生成，如 "sec_" + H2标题的哈希值。
     * 此ID会被其下的 {@link Block} 对象引用 ({@link Block#parentHeadingId})。
     */
    private String sectionId;

    /**
     * 节的标题信息。
     * 通常是 H2 级别的标题。如果一个节是由更低级别标题（如H3直接出现在H1下）虚拟创建的，
     * 那么这里的 heading level 可能是该低级别，具体取决于解析逻辑。
     * @see Heading
     */
    private Heading heading;

    /**
     * 节包含的内容块列表。
     * 这包括段落 ({@link com.ral.young.spring.ai.model.block.Paragraph})、
     * 表格 ({@link com.ral.young.spring.ai.model.block.Table})、
     * 图片 ({@link com.ral.young.spring.ai.model.block.Image}) 等。
     * H3-H6 标题本身也可以被视为一种特殊的块或其下的内容直接加入此列表。
     * @see Block
     */
    private List<Block> blocks;

    /**
     * 此节所属的章的ID ({@link Chapter#chapterId})。
     * 用于将节与正确的章关联起来。
     */
    private String chapterId;
} 