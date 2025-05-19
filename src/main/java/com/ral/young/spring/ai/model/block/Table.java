package com.ral.young.spring.ai.model.block;

import com.ral.young.spring.ai.model.Position;
import com.ral.young.spring.ai.model.enums.BlockType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表格实体。
 * 表示文档中的一个结构化表格，继承自 {@link Block}。
 * 包含行数据 ({@link TableRow} 列表)、表格标题 (caption)、行列数等信息。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Table extends Block {
    /**
     * 表格的行数据列表。
     * 每行数据由一个 {@link TableRow} 对象表示。
     * @see TableRow
     */
    private List<TableRow> rows;

    /**
     * 表格标题或说明文字 (Caption)。
     * 通常是 Word 中为表格添加的题注内容，可能位于表格上方或下方。
     * 解析时需要从文档中提取与表格关联的这类文本。
     */
    private String caption;

    /**
     * 表格的列数。
     * 通常根据第一行（或表头行）的单元格数量确定。
     * 注意：对于包含合并单元格 (merged cells) 的复杂表格，此计数可能需要更复杂的逻辑来准确反映表格结构。
     * 简单实现中，可以取最大单元格数或首行单元格数。
     */
    private int columnCount;

    /**
     * 表格的行数。
     * 直接对应 {@link #rows} 列表的大小。
     */
    private int rowCount;

    /**
     * 构造一个新的表格实例。
     * @param blockId 表格的唯一标识符。
     * @param rows 表格的行数据列表。
     * @param caption 表格的标题或说明。
     * @param position 表格的位置信息（可选）。
     */
    public Table(String blockId, List<TableRow> rows, String caption, Position position) {
        this.setBlockId(blockId);
        this.setType(BlockType.TABLE);
        this.rows = rows;
        this.caption = caption;
        this.setPosition(position);
        if (rows != null && !rows.isEmpty()) {
            this.rowCount = rows.size();
            // 假设所有行具有相同列数或取第一行的列数作为代表。对于有合并单元格的表格，此假设可能不完全准确。
            this.columnCount = rows.get(0).getCells() != null ? rows.get(0).getCells().size() : 0;
        } else {
            this.rowCount = 0;
            this.columnCount = 0;
        }
    }
} 