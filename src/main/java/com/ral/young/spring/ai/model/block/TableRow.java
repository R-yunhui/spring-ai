package com.ral.young.spring.ai.model.block;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 表格行实体。
 * 代表 {@link Table} 中的单行数据。
 * 包含该行所有单元格的内容（简化为字符串列表）以及一个指示是否为表头行的标志。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableRow {
    /**
     * 单元格内容列表。
     * 每个字符串代表一个单元格的文本内容。对于复杂单元格（例如，包含多个段落、图片或嵌套表格的单元格），
     * 此简化模型可能不足，实际解析时可能需要将单元格内容进一步解析为 {@link Block} 列表。
     * 当前模型假定单元格只包含纯文本。
     */
    private List<String> cells;

    /**
     * 是否为表头行。
     * 如果为 true，表示此行为表格的标题行（header row）。
     * Word 中表头行可能有特殊的标记或样式，解析时需要识别。
     */
    private boolean isHeaderRow;
} 