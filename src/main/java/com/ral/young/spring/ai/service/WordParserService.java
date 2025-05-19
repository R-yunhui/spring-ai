package com.ral.young.spring.ai.service;

import com.ral.young.spring.ai.model.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Word 文档解析服务接口。
 * 定义了将 Word 文档（.docx格式）解析为结构化 {@link Document} 对象的操作。
 */
public interface WordParserService {

    /**
     * 解析上传的 Word (.docx) 文件。
     *
     * @param file 上传的 MultipartFile 对象，代表 .docx 文件。
     * @return 解析后的 {@link Document} 对象，包含文档的结构化内容。
     * @throws IOException 如果读取文件或解析过程中发生 I/O 错误。
     * @throws IllegalArgumentException 如果文件格式不受支持或文件为空。
     */
    Document parseWordDocument(MultipartFile file) throws IOException;

    /**
     * 从输入流解析 Word (.docx) 文档。
     *
     * @param inputStream 包含 .docx 文件内容的输入流。
     * @param originalFilename 原始文件名（可选，用于生成 docId 或 title）。
     * @return 解析后的 {@link Document} 对象。
     * @throws IOException 如果读取输入流或解析过程中发生 I/O 错误。
     * @throws IllegalArgumentException 如果输入流为空。
     */
    Document parseWordDocument(InputStream inputStream, String originalFilename) throws IOException;
} 