package com.ral.young.spring.ai.model.block;

import com.ral.young.spring.ai.model.Position;
import com.ral.young.spring.ai.model.enums.BlockType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 图片实体。
 * 表示文档中嵌入的一张图片，继承自 {@link Block}。
 * 包含图片的文件名（如果可获取）、内容类型 (MIME type)、实际的二进制数据以及可选的描述文本 (alt text)。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Image extends Block {

    /**
     * 图片的文件名。
     * 在 Word 文档中，嵌入的图片可能有原始文件名或建议的文件名，解析时可尝试获取。
     * 如果无法获取，可以基于图片类型或ID生成一个。
     */
    private String fileName;

    /**
     * 图片的MIME内容类型。
     * 例如 "image/jpeg", "image/png", "image/gif"等。
     * Apache POI 通常可以提供此信息。
     */
    private String contentType;

    /**
     * 图片的二进制数据。
     * 直接存储图片的字节内容，可以用于后续保存或处理。
     */
    private byte[] data;

    /**
     * 图片的描述或替代文本 (Alt Text)。
     * 用于辅助功能，或者在图片无法正常显示时提供文字说明。
     * Word 中可以为图片设置替换文字。
     */
    private String description;

    /**
     * 构造一个新的图片实例。
     * @param blockId 图片的唯一标识符。
     * @param fileName 图片的文件名（可为null）。
     * @param contentType 图片的MIME类型。
     * @param data 图片的二进制数据。
     * @param description 图片的描述文本（可为null）。
     * @param position 图片的位置信息（可选）。
     */
    public Image(String blockId, String fileName, String contentType, byte[] data, String description, Position position) {
        this.setBlockId(blockId);
        this.setType(BlockType.IMAGE);
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.description = description;
        this.setPosition(position);
    }
} 