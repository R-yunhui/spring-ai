package com.ral.young.spring.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * 文档元数据实体。
 * 存储关于Word文档本身的附加信息，如作者、创建日期、修改日期等。
 * 这些信息通常可以从文档的属性中提取。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {
    /**
     * 文档的作者。
     */
    private String author;

    /**
     * 文档的创建日期和时间。
     */
    private Date creationDate;

    /**
     * 文档的最后修改日期和时间。
     */
    private Date lastModifiedDate;

    /**
     * 创建或最后修改文档的应用程序名称。
     * 例如 "Microsoft Office Word"。
     */
    private String applicationName;

    /**
     * 文档的标题属性 (可能不同于文件名或文档内的第一个H1)。
     * POI 可以从文档属性中获取。
     */
    private String documentTitleProperty;

    /**
     * 其他自定义的文档属性。
     * 以键值对形式存储，键是属性名，值是属性值。
     */
    private Map<String, String> customProperties;

    // 可以根据需要添加更多标准或自定义的元数据字段，例如：
    // private String keywords;
    // private String subject;
    // private int pageCount; (获取精确页数可能需要特定处理)
} 