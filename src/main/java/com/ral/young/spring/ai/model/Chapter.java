package com.ral.young.spring.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 章实体。
 * 对应文档中的一级标题 (H1) 及其下的内容。
 * 一个章包含一个主标题 ({@link Heading}) 和一个节 ({@link Section}) 列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chapter {
    /**
     * 章的唯一标识符。
     * 根据设计文档，为 "ch_" + H1标题的哈希值。
     * 此ID会被其下的 {@link Section} 对象引用 ({@link Section#chapterId})，
     * 并且如果一个 {@link com.ral.young.spring.ai.model.block.Block} 直接属于章（没有中间的节），
     * 也会被该 Block 引用 ({@link com.ral.young.spring.ai.model.block.Block#parentHeadingId})。
     */
    private String chapterId;

    /**
     * 章的标题信息。
     * 通常是 H1 级别的主标题。
     * @see Heading
     */
    private Heading heading;

    /**
     * 章包含的节列表。
     * 每个 {@link Section} 对象代表该章下的一个二级结构单元 (通常由H2标题开始)。
     * 如果章下面直接是内容块而不是由H2定义的节，解析逻辑可能需要创建一个默认的节或直接将块关联到章。
     * @see Section
     */
    private List<Section> sections;

    /**
     * 此章所属的文档的ID ({@link Document#docId})。
     * 用于将章与正确的文档关联起来。
     */
    private String documentId;
} 