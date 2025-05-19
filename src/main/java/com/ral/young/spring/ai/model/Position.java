package com.ral.young.spring.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 位置信息实体。
 * 用于描述文档元素在页面中的大致位置。实际精确获取页码和坐标对 POI 可能是挑战。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    /**
     * 元素所在的页码。
     * 注意：Apache POI 对于复杂文档结构，直接获取页码可能不准确或不支持。
     * 此字段更多作为概念保留，实际填充可能需要高级布局分析或第三方库。
     */
    private int pageNum;

    /**
     * 元素在页面中的矩形区域坐标 [x1, y1, x2, y2]。
     * 注意：与 pageNum 类似，精确坐标获取对于 POI 可能是挑战。
     * 值的单位和坐标系依赖于具体的解析实现和源文档格式。
     */
    private List<Double> rect;
} 