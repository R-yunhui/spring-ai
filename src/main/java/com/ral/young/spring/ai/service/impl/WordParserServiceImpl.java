package com.ral.young.spring.ai.service.impl;

import com.ral.young.spring.ai.model.*;
import com.ral.young.spring.ai.model.Document;
import com.ral.young.spring.ai.model.block.*;
import com.ral.young.spring.ai.model.enums.BlockType;
import com.ral.young.spring.ai.service.WordParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Word 文档解析服务实现类。
 * 使用 Apache POI 解析 .docx 文件，将其内容转换为结构化的 {@link Document} 对象。
 */
@Service
@Slf4j
public class WordParserServiceImpl implements WordParserService {

    private static final String HEADING_STYLE_PREFIX = "heading";
    // Word 内置标题样式通常有特定的 styleId，例如 "Heading1", "Heading2" (英文版)
    // 或者中文版可能是 "1", "2" 等数字，或特定名称。需要根据实际情况调整或提供更灵活的匹配方式。
    // 为了通用性，我们假设可以通过获取段落样式ID，并检查其是否以 "heading" (或其他语言的"标题")开头，
    // 或者直接检查段落的 getStyleID() 返回值。
    // POI 中 XWPFParagraph.getStyleID() 可以获取样式ID。
    // XWPFParagraph.getNumIlvl() 可以获取列表级别，有时也用于标题。

    @Override
    public Document parseWordDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn("尝试解析的 Word 文件为空或未提供。");
            throw new IllegalArgumentException("文件不能为空");
        }
        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".docx")) {
            log.warn("尝试解析不支持的文件格式: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("仅支持 .docx 文件格式");
        }
        log.info("开始解析 Word 文档: {}", file.getOriginalFilename());
        // 对于MultipartFile，每次getInputStream()通常返回新的独立的流，更安全
        // 但为了确保MD5在原始数据上计算，并避免POI重复读取可能存在的消耗问题，
        // 理想情况下应该先读取到byte[]，但MultipartFile通常不建议这样做（内存）。
        // 这里假设 getInputStream() 多次调用是安全的。
        String docId;
        try (InputStream forMd5 = file.getInputStream()) {
            docId = DigestUtils.md5DigestAsHex(forMd5);
        }
        try (InputStream streamForPoi = file.getInputStream()){
            return parseWordDocumentInternal(streamForPoi, file.getOriginalFilename(), docId);
        } catch (IOException e) {
            log.error("解析 Word 文档 {} 失败: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Document parseWordDocument(InputStream inputStream, String originalFilename) throws IOException {
        if (inputStream == null) {
            log.warn("尝试解析的 Word 文件输入流为空。");
            throw new IllegalArgumentException("输入流不能为空");
        }
        // 当传入的是任意InputStream时，我们无法保证它可以被reset。
        // 为了计算MD5，需要读取流。如果流不可重置，POI将无法读取。
        // 最佳实践是调用者负责传入可以多次读取的流（如ByteArrayInputStream）或预先计算docId。
        // 这里为了简化，如果文件名存在则用文件名md5，否则用UUID，这表示docId的来源可能不一致。
        String docId = (originalFilename != null && !originalFilename.isEmpty()
                ? DigestUtils.md5DigestAsHex(originalFilename.getBytes())
                : UUID.randomUUID().toString());
        log.info("开始从输入流解析 Word 文档: {} (DocID: {})", originalFilename, docId);
        return parseWordDocumentInternal(inputStream, originalFilename, docId);
    }

    private Document parseWordDocumentInternal(InputStream inputStream, String originalFilename, String docId) throws IOException {
        XWPFDocument xwpfDocument;
        try {
            xwpfDocument = new XWPFDocument(inputStream);
        } catch (IOException e) {
            log.error("无法使用 Apache POI 打开 Word 文档流 ({}): {}", originalFilename, e.getMessage(), e);
            throw new IOException("打开 Word 文档失败: " + e.getMessage(), e);
        }

        Document document = new Document();
        document.setDocId(docId);
        document.setTitle(originalFilename != null ? originalFilename.replaceAll("\\.docx$", "") : "Untitled Document");
        document.setMetadata(extractMetadata(xwpfDocument));

        List<Chapter> chapters = new ArrayList<>();
        document.setChapters(chapters);

        Chapter currentChapter = null;
        Section currentSection = null;
        AtomicInteger blockIdCounter = new AtomicInteger(0); // 用于生成唯一的 blockId (不包括图片)

        // 遍历文档主体内容元素 (段落和表格)
        for (IBodyElement bodyElement : xwpfDocument.getBodyElements()) {
            if (bodyElement instanceof XWPFParagraph paragraph) {
				String paragraphTextContent = paragraph.getText();

                // 1. 首先，从当前段落提取所有图片
                // extractImagesFromParagraph 内部会处理图片的 blockId 生成和计数器递增
                List<Image> imagesInParagraph = extractImagesFromParagraph(paragraph, xwpfDocument, docId, blockIdCounter);
                for (Image img : imagesInParagraph) {
                    assignBlockToStructure(img, currentSection, currentChapter, docId, chapters, originalFilename);
                    log.debug("图片 {} (ID: {}) 被提取并添加到文档结构中。", (img.getFileName() != null ? img.getFileName() : "N/A"), img.getBlockId());
                }

                // 2. 然后，处理段落的文本内容，判断是否为标题
                int headingLevel = getHeadingLevel(paragraph);

                if (headingLevel > 0) { // 是一个标题
                    Heading headingObject = createHeading(paragraphTextContent, headingLevel, docId);
                    if (headingLevel == 1) { // H1 - 新章节
                        currentChapter = new Chapter();
                        currentChapter.setChapterId(headingObject.getId());
                        currentChapter.setHeading(headingObject);
                        currentChapter.setSections(new ArrayList<>());
                        currentChapter.setDocumentId(docId);
                        chapters.add(currentChapter);
                        currentSection = null; // 新章节开始，清空当前节
                        log.debug("创建新章节: {} (ID: {})", headingObject.getText(), headingObject.getId());
                    } else { // H2-H6
                        if (currentChapter == null) {
                            // 如果没有H1，则创建一个默认章节
                            currentChapter = createDefaultChapter(docId, chapters);
                            log.warn("文档 {} 在H{} '{}' 前缺少H1标题，已创建默认章节。", originalFilename, headingLevel, headingObject.getText());
                        }
                        if (headingLevel == 2) { // H2 - 新节
                            currentSection = new Section();
                            currentSection.setSectionId(headingObject.getId());
                            currentSection.setHeading(headingObject);
                            currentSection.setBlocks(new ArrayList<>());
                            currentSection.setChapterId(currentChapter.getChapterId());
                            currentChapter.getSections().add(currentSection);
                            log.debug("创建新节: {} (ID: {}) 于章节 ID: {}", headingObject.getText(), headingObject.getId(), currentChapter.getChapterId());
                        } else { // H3-H6 标题作为文本块处理
                            if (currentSection == null) {
                                // 如果H3-H6出现在H2之前 (或H1下无H2直接出现H3+)
                                // 创建一个以该H3-H6为标题的节 (此处的headingObject是H3-H6的)
                                currentSection = createDefaultSectionForSubHeading(currentChapter, headingObject, docId);
                                log.warn("H{} '{}' (ID: {}) 出现前无H2，已在章节ID: {} 下创建默认节。", headingLevel, headingObject.getText(), headingObject.getId(), currentChapter.getChapterId());
                            }
                            // 将H3-H6标题本身的文本视为一个段落块，添加到当前节 (如果它有文本)
                            if (!paragraphTextContent.trim().isEmpty()) {
                                String headingTextBlockId = "blk_txt_head_" + docId + "_" + blockIdCounter.incrementAndGet();
                                Paragraph headingParagraph = createParagraphFromXWPF(paragraph, headingTextBlockId, docId);
                                assignBlockToStructure(headingParagraph, currentSection, currentChapter, docId, chapters, originalFilename);
                                log.debug("H{} '{}' 的文本作为段落块 (ID: {}) 添加到节 ID: {}", headingLevel, headingObject.getText(), headingParagraph.getBlockId(), currentSection.getSectionId());
                            }
                        }
                    }
                } else { // 普通段落 (不是标题)
                    // 只有在段落包含实际文本时才创建 Paragraph 对象
                    // 图片已在前面作为独立的 Image Block 处理
                    if (!paragraphTextContent.trim().isEmpty()) {
                        String contentParagraphBlockId = "blk_txt_para_" + docId + "_" + blockIdCounter.incrementAndGet();
                        Paragraph contentParagraph = createParagraphFromXWPF(paragraph, contentParagraphBlockId, docId);
                        assignBlockToStructure(contentParagraph, currentSection, currentChapter, docId, chapters, originalFilename);
                        log.debug("段落 '{}...' (ID: {}) 被添加到文档结构中。", contentParagraph.getText().substring(0, Math.min(contentParagraph.getText().length(), 20)), contentParagraph.getBlockId());
                    }
                }
            } else if (bodyElement instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) bodyElement;
                String tableBlockId = "blk_tbl_" + docId + "_" + blockIdCounter.incrementAndGet();
                Table contentTable = createTableFromXWPF(table, tableBlockId, docId);
                assignBlockToStructure(contentTable, currentSection, currentChapter, docId, chapters, originalFilename);
                log.debug("表格 (ID: {}) ({}x{}) 被提取并添加到文档结构中。", contentTable.getBlockId(), contentTable.getRowCount(), contentTable.getColumnCount());
            }
        }

        // 处理没有标准标题结构但有内容的文档，或完全空文档
        if (chapters.isEmpty() && currentChapter == null) { // No H1 was encountered
            boolean hasContent = xwpfDocument.getBodyElements().stream().anyMatch(be -> {
                if (be instanceof XWPFParagraph) {
                    // Consider content if it has text OR if it results in images
                    // This requires checking images again or trusting blockIdCounter state, simpler to check text directly
                    if (!((XWPFParagraph)be).getText().trim().isEmpty()) return true;
                    // A paragraph might only contain an image. This case is handled by image extraction above for the main loop.
                    // For this default handling, we assume if images were the *only* content, they'd need a default structure.
                    // However, extractImagesFromParagraph is not called again here for simplicity.
                    // Focus on text and tables for "hasContent" in this specific fallback.
                    // A more robust `hasContent` would re-evaluate images if needed, but adds complexity.
                }
                if (be instanceof XWPFTable) return ((XWPFTable)be).getNumberOfRows() > 0;
                return false;
            });

            // A check for whether any blocks (images, specifically) were created even if no chapters/sections were.
            // This is tricky because blocks are added to currentSection/currentChapter which might be null.
            // The assignBlockToStructure creates default structures if they are null.
            // So, if assignBlockToStructure was called at least once, chapters list wouldn't be empty.
            // Thus, this `if (chapters.isEmpty())` implies no blocks were successfully assigned.

            if (hasContent || !document.getChapters().isEmpty()) { // If there was text/table content OR if images created default structures
                log.warn("文档 {} 不包含任何标准H1标题。将所有内容放入默认章节和默认节。", originalFilename);
                // Ensure default chapter/section exist if blocks were added without explicit H1/H2
                if (!document.getChapters().isEmpty()) { // Images might have created a default chapter
                    currentChapter = document.getChapters().getFirst(); // Assume first one
                    if (currentChapter.getSections().isEmpty()) {
                        currentSection = createDefaultSection(currentChapter, docId, "默认节内容");
                    } else {
                        currentSection = currentChapter.getSections().get(0); // Assume first section
                    }
                } else { // No content at all created any structure
                    currentChapter = createDefaultChapter(docId, chapters);
                    currentSection = createDefaultSection(currentChapter, docId, "默认节内容");
                }


                // If we reached here because of `hasContent` but structures were not made by images,
                // we need to re-parse to put content into the newly created default chapter/section.
                // This re-parsing is only if chapters list was initially empty.
                if (document.getChapters().getFirst().getSections().getFirst().getBlocks().isEmpty() && hasContent) {
                    Section targetSection = currentSection; // The default section created
                    log.info("重新遍历内容以放入默认结构中 (文档: {})", originalFilename);
                    for (IBodyElement bodyElement : xwpfDocument.getBodyElements()) {
                        if (bodyElement instanceof XWPFParagraph) {
                            XWPFParagraph p = (XWPFParagraph) bodyElement;
                            // Images from this paragraph (if any) would have been processed in the main loop.
                            // If they were, they would have created their own default structure if needed.
                            // Here, we only care about text content for paragraphs if not already handled.
                            // This re-scan is tricky because images might be re-extracted.
                            // To avoid double-adding images, only add text here.
                            if (!p.getText().trim().isEmpty()) {
                                String paraBlockId = "blk_def_txt_" + docId + "_" + blockIdCounter.incrementAndGet();
                                Paragraph para = createParagraphFromXWPF(p, paraBlockId, docId);
                                // Manually assign and add, as assignBlockToStructure might create *another* default.
                                para.setParentHeadingId(targetSection.getSectionId());
                                para.setOrderInParent(targetSection.getBlocks().size());
                                targetSection.getBlocks().add(para);
                            }
                        } else if (bodyElement instanceof XWPFTable) {
                            String tblBlockId = "blk_def_tbl_" + docId + "_" + blockIdCounter.incrementAndGet();
                            Table tbl = createTableFromXWPF((XWPFTable) bodyElement, tblBlockId, docId);
                            tbl.setParentHeadingId(targetSection.getSectionId());
                            tbl.setOrderInParent(targetSection.getBlocks().size());
                            targetSection.getBlocks().add(tbl);
                        }
                    }
                }
            } else if (chapters.isEmpty()) { // Truly empty or only unrecognized content
                log.warn("文档 {} 为空或不包含可识别的内容来创建默认章节。将创建一个空的默认章节。", originalFilename);
                createDefaultChapter(docId, chapters); // Ensure at least one chapter
            }
        }

        log.info("Word 文档 {} 解析完成。共解析 {} 个章节。", originalFilename, chapters.size());
        return document;
    }

    private DocumentMetadata extractMetadata(XWPFDocument xwpfDocument) {
        DocumentMetadata metadata = new DocumentMetadata();
        try {
            org.apache.poi.ooxml.POIXMLProperties.CoreProperties coreProps = xwpfDocument.getProperties().getCoreProperties();
            metadata.setAuthor(coreProps.getCreator());
            metadata.setCreationDate(coreProps.getCreated());
            metadata.setLastModifiedDate(coreProps.getModified());
            metadata.setDocumentTitleProperty(coreProps.getTitle());

            org.apache.poi.ooxml.POIXMLProperties.ExtendedProperties extProps = xwpfDocument.getProperties().getExtendedProperties();
            if (extProps != null) {
                metadata.setApplicationName(extProps.getApplication());
            }

            // 自定义属性提取 (如果需要)
            // org.apache.poi.ooxml.POIXMLProperties.CustomProperties custProps = xwpfDocument.getProperties().getCustomProperties();
            // if (custProps != null && custProps.getUnderlyingProperties() != null) {
            //     Map<String, String> customMap = new HashMap<>();
            //     for (org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty prop : custProps.getUnderlyingProperties().getPropertyList()) {
            //         customMap.put(prop.getName(), extractCustomPropertyValue(prop));
            //     }
            //     metadata.setCustomProperties(customMap);
            // }

        } catch (Exception e) {
            log.warn("提取文档元数据时发生错误: {}", e.getMessage());
        }
        return metadata;
    }

    // private String extractCustomPropertyValue(org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty prop) {
    //    if (prop.isSetLpwstr()) return prop.getLpwstr();
    //    if (prop.isSetFiletime()) return prop.getFiletime().toString();
    //    if (prop.isSetDate()) return prop.getDate().toString();
    //    if (prop.isSetBool()) return String.valueOf(prop.getBool());
    //    // ... 其他类型 i4, r8 等
    //    return "[unsupported custom property type]";
    // }

    private int getHeadingLevel(XWPFParagraph paragraph) {
        String styleId = paragraph.getStyleID();
        if (styleId != null) {
            // 检查Word内置的数字样式ID (例如 "1", "2" ... "9" 对应 标题1..标题9)
            try {
                // 尝试将styleId直接转为数字，这在某些中文版Word的标题样式中常见
                int level = Integer.parseInt(styleId);
                if (level >=1 && level <=9) return level; // 假设1-9是标题级别
            } catch (NumberFormatException e) {
                // styleId 不是纯数字，继续其他检查
            }

            // 检查以 "heading" (不区分大小写) 开头的样式ID，并提取后面的数字
            // 例如 "Heading1", "heading 2", "header3" 等
            String lowerStyleId = styleId.toLowerCase();
            String[] prefixes = {"heading", "header", "h", "标题"}; // 添加中文"标题"
            for (String prefix : prefixes) {
                if (lowerStyleId.startsWith(prefix)) {
                    String numericalPart = lowerStyleId.substring(prefix.length()).trim();
                    if (numericalPart.matches("\\d+")) {
                        try {
                            int level = Integer.parseInt(numericalPart);
                            if (level >= 1 && level <= 9) return level; // 常见的标题级别
                        }
                        catch (NumberFormatException ignored) { }
                    } else if (numericalPart.isEmpty() && prefix.equals("h")) {
                        // Handle cases like "h" followed by a number in the text, e.g. run.getText() == "1"
                        // This is too complex and unreliable here. Style ID should be definitive.
                    }
                }
            }
        }

        // 检查段落大纲级别 (Outline Level) - CTPPr is part of ooxml-schemas, not directly in XWPFParagraph easily
        // org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr ppr = paragraph.getCTP().getPPr();
        // if (ppr != null && ppr.isSetOutlineLvl()) {
        //     return ppr.getOutlineLvl().getVal().intValue() + 1; // 大纲级别从0开始
        // }

        // XWPFParagraph.getNumIlvl() for list item levels. Sometimes used for headings.
        // BigInteger numIlvl = paragraph.getNumIlvl();
        // if (numIlvl != null) {
        //    // Could check paragraph.getNumID() and then XWPFDocument.getNumbering().getNum(paragraph.getNumID())
        //    // to see if the numbering style itself is a heading style. Complex.
        // }

        return 0; // 不是标题或无法识别
    }

    private Heading createHeading(String text, int level, String docId) {
        String cleanText = text.trim();
        // 根据设计文档，chapterId 是 "ch_" + H1标题哈希
        // sectionId 类似，例如 "sec_" + H2标题哈希
        // Adding docId prefix for global uniqueness if multiple docs are processed.
        String idSuffix = DigestUtils.md5DigestAsHex(cleanText.getBytes()).substring(0,12);
        String headingIdPrefix = level == 1 ? "ch_" : (level == 2 ? "sec_" : "sub_");
        String headingId = headingIdPrefix + docId.substring(0, Math.min(docId.length(),8)) + "_" + idSuffix;
        return new Heading(level, cleanText, headingId);
    }

    private Paragraph createParagraphFromXWPF(XWPFParagraph xwpfParagraph, String blockId, String docId) {
        Paragraph p = new Paragraph();
        p.setBlockId(blockId);
        p.setText(xwpfParagraph.getText()); // 获取完整纯文本
        // parentHeadingId and orderInParent will be set by assignBlockToStructure
        p.setType(BlockType.PARAGRAPH);

        List<Span> spans = new ArrayList<>();
        for (XWPFRun run : xwpfParagraph.getRuns()) {
            String runText = run.getText(0); // getText(0) 获取run的文本
            if (runText == null || runText.isEmpty()) {
                // Run might contain only an image or other non-textual content, skip span creation if no text.
                // Images are handled separately.
                continue;
            }
            Span span = new Span();
            span.setText(runText);

            Map<String, Object> style = new HashMap<>();
            if (run.isBold()) style.put("bold", true);
            if (run.isItalic()) style.put("italic", true);
            if (run.getUnderline() != UnderlinePatterns.NONE && run.getUnderline() != null) style.put("underline", run.getUnderline().toString().toLowerCase());
            if (run.getColor() != null) style.put("color", run.getColor()); // Hex RGB
            if (run.getFontFamily() != null) style.put("fontFamily", run.getFontFamily());
            // XWPFRun.getFontSizeAsDouble() 返回点单位的字号，如果未设置，则为-1.0或null
            Double fontSize = run.getFontSizeAsDouble();
            style.put("fontSize", (fontSize == null || fontSize == -1.0) ? "default" : fontSize);
            // 更多样式... CharacterSpacing, Kerning, etc.
            span.setStyle(style);
            spans.add(span);
        }
        p.setSpans(spans);
        // p.setPosition(extractPosition(xwpfParagraph)); // 位置信息提取较复杂，暂缓
        return p;
    }

    private Table createTableFromXWPF(XWPFTable xwpfTable, String blockId, String docId) {
        Table t = new Table();
        t.setBlockId(blockId);
        // parentHeadingId and orderInParent will be set by assignBlockToStructure
        t.setType(BlockType.TABLE);

        List<TableRow> tableRows = new ArrayList<>();
        List<XWPFTableRow> xwpfRows = xwpfTable.getRows();

        boolean firstRowIsHeader = false;
        if (!xwpfRows.isEmpty()) {
            XWPFTableRow firstRow = xwpfRows.get(0);
            // Heuristic: if the first row is marked as "repeat header" or cells have specific header formatting.
            // CTTblPrEx tblPrEx = xwpfTable.getCTTbl().getTblPr().getTblPrEx();
            // if (tblPrEx != null && tblPrEx.isSetTblHeader()) firstRowIsHeader = tblPrEx.getTblHeader().getVal().equals(STOnOff.ON);
            // A simpler check: XWPFTableRow.isCantSplitRow() is sometimes used for headers, but not reliably.
            // Another simple heuristic: check if all cells in the first row are bold.
            // For now, use a very simple heuristic: if it's marked as "isCantSplitRow"
            firstRowIsHeader = firstRow.isCantSplitRow();
        }


        for (int i = 0; i < xwpfRows.size(); i++) {
            XWPFTableRow xwpfRow = xwpfRows.get(i);
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell xwpfCell : xwpfRow.getTableCells()) {
                cells.add(xwpfCell != null ? (xwpfCell.getTextRecursively() != null ? xwpfCell.getTextRecursively() : xwpfCell.getText()) : "");
            }
            tableRows.add(new TableRow(cells, i == 0 && firstRowIsHeader));
        }
        t.setRows(tableRows);
        if (!tableRows.isEmpty() && !tableRows.get(0).getCells().isEmpty()) {
            t.setRowCount(tableRows.size());
            t.setColumnCount(tableRows.get(0).getCells().size());
        } else {
            t.setRowCount(tableRows.size());
            t.setColumnCount(0);
        }
        // t.setCaption(extractTableCaption(xwpfTable)); // 表格标题提取需要逻辑
        // t.setPosition(extractPosition(xwpfTable)); // 位置信息提取较复杂，暂缓
        return t;
    }

    /**
     * 从段落中提取图片，包括内嵌图片和 DrawingML 中的图片。
     * @param paragraph 要解析的段落
     * @param xwpfDocument Word文档对象，用于通过ID获取图片数据
     * @param docId 文档ID
     * @param imageIdCounter 用于生成图片唯一ID的计数器
     * @return 提取到的图片列表
     */
    private List<Image> extractImagesFromParagraph(XWPFParagraph paragraph, XWPFDocument xwpfDocument, String docId, AtomicInteger imageIdCounter) {
        List<Image> images = new ArrayList<>();
        if (paragraph == null) {
            return images;
        }

        // 1. 提取内嵌图片 (XWPFRun -> XWPFPicture)
        for (XWPFRun run : paragraph.getRuns()) {
            if (run == null) continue;
            try {
                for (XWPFPicture picture : run.getEmbeddedPictures()) {
                    if (picture == null) continue;
                    XWPFPictureData pictureData = picture.getPictureData();
                    if (pictureData != null) {
                        String imageBlockId = "img_emb_" + docId + "_" + imageIdCounter.incrementAndGet();
                        Image image = new Image(
                                imageBlockId,
                                pictureData.getFileName(),
                                pictureData.getPictureTypeEnum().getContentType(),
                                pictureData.getData(),
                                picture.getDescription(), // Alt text from XWPFPicture
                                null // Position, 暂不提取
                        );
                        images.add(image);
                        log.debug("提取到内嵌图片: {}, ID: {}", (image.getFileName() != null ? image.getFileName() : "N/A"), image.getBlockId());
                    }
                }
            } catch (Exception e) {
                // POI的 run.getEmbeddedPictures() 有时可能因底层XML问题抛出异常
                log.warn("提取段落内嵌图片时发生错误 (Run: '{}'): {}", run.getText(0), e.getMessage());
            }
        }

        // 2. 提取 DrawingML 中的图片 (浮动图片等)
        // CTP (段落) -> CTR (Run) -> CTDrawing -> CTInline/CTAnchor -> CTPicture
        CTP ctp = paragraph.getCTP();
        if (ctp == null) {
            return images; // 如果没有底层CTP对象，无法继续
        }

        for (CTR ctr : ctp.getRList()) {
            if (ctr == null) continue;
            // 一个 CTR (Run) 可能包含多个 CTDrawing 对象
            for (org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing ctDrawing : ctr.getDrawingList()) {
                if (ctDrawing == null) continue;

                // 处理内联形状 (CTInline)
                for (CTInline inline : ctDrawing.getInlineList()) {
                    if (inline != null && inline.getGraphic() != null && inline.getGraphic().getGraphicData() != null) {
                        CTPicture pic = getPicFromGraphicData(inline.getGraphic().getGraphicData());
                        if (pic != null) {
                            addPictureToList(pic, xwpfDocument, images, docId, imageIdCounter, "inline");
                        }
                    }
                }
                // 处理锚定形状 (CTAnchor - 通常是浮动图片)
                for (CTAnchor anchor : ctDrawing.getAnchorList()) {
                    if (anchor != null && anchor.getGraphic() != null && anchor.getGraphic().getGraphicData() != null) {
                        CTPicture pic = getPicFromGraphicData(anchor.getGraphic().getGraphicData());
                        if (pic != null) {
                            addPictureToList(pic, xwpfDocument, images, docId, imageIdCounter, "anchor");
                        }
                    }
                }
            }
        }
        return images;
    }

    // 辅助方法从 GraphicData 中获取 CTPicture
    private CTPicture getPicFromGraphicData(org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObjectData graphicData) {
        if (graphicData == null) {
            return null;
        }
        // 尝试通过遍历 graphicData 的子元素来查找 CTPicture
        // CTPicture 的 QName: {http://schemas.openxmlformats.org/drawingml/2006/picture}pic
        for (XmlObject xmlObject : graphicData.selectChildren(
                new javax.xml.namespace.QName("http://schemas.openxmlformats.org/drawingml/2006/picture", "pic"))) {
            if (xmlObject instanceof CTPicture) {
                return (CTPicture) xmlObject;
            }
        }

        // 如果直接子元素遍历失败，尝试使用 XPath (更通用但可能稍慢)
        // XPath: declare namespace pic='http://schemas.openxmlformats.org/drawingml/2006/picture' .//pic:pic
        // 这会在 graphicData 的任何后代中查找 pic:pic 元素
        try {
            XmlObject[] picts = graphicData.selectPath("declare namespace pic='http://schemas.openxmlformats.org/drawingml/2006/picture' .//pic:pic");
            if (picts != null && picts.length > 0 && picts[0] instanceof CTPicture) {
                return (CTPicture) picts[0];
            }
        } catch (Exception e) {
            log.warn("使用 XPath 从 CTGraphicalObjectData 提取 CTPicture 失败: {}", e.getMessage());
        }
        return null;
    }

    // 辅助方法将 CTPicture 添加到 Image 列表
    private void addPictureToList(CTPicture pic, XWPFDocument document, List<Image> images, String docId, AtomicInteger imageIdCounter, String type) {
        if (pic == null) {
            // log.warn("尝试添加的 CTPicture 为 null (类型: {})", type); // Too verbose for default
            return;
        }
        if (pic.getBlipFill() == null || pic.getBlipFill().getBlip() == null) {
            log.warn("DrawingML CTPicture (类型: {}) 的 BlipFill 或 Blip 为 null。文档可能损坏或图片格式特殊。", type);
            return;
        }

        String blipEmbedId = pic.getBlipFill().getBlip().getEmbed();
        if (blipEmbedId == null || blipEmbedId.isEmpty()) {
            log.warn("DrawingML CTPicture (类型: {}) Blip embed ID (r:embed) 为 null 或空。", type);
            return;
        }

        XWPFPictureData pictureData = null;
        try {
            // XWPFDocument.getPictureDataByID 需要的是关系ID (rId)
            pictureData = document.getPictureDataByID(blipEmbedId);
        } catch (Exception e) {
            // This can happen if the r:embed ID is not found in the document's relationships part,
            // or if the part it points to is not a valid image type POI recognizes.
            log.warn("通过 ID '{}' 从文档获取图片数据失败 (DrawingML 类型: {}): {}. 图片可能已损坏、引用无效或格式不受支持。", blipEmbedId, type, e.getMessage());
        }

        if (pictureData != null) {
            String imageBlockId = "img_drw_" + type + "_" + docId.substring(0, Math.min(docId.length(), 4)) + "_" + imageIdCounter.incrementAndGet();
            String description = null;
            if (pic.getNvPicPr() != null &&
                    pic.getNvPicPr().getCNvPr() != null &&
                    pic.getNvPicPr().getCNvPr().isSetDescr()) {
                description = pic.getNvPicPr().getCNvPr().getDescr();
            }

            Image image = new Image(
                    imageBlockId,
                    pictureData.getFileName(), // 文件名可能为 null 或通用名称
                    pictureData.getPictureTypeEnum().getContentType(), // 使用 getContentType()
                    pictureData.getData(),
                    description, // 图片的描述文本 (alt text)
                    null // Position, 暂不提取
            );
            images.add(image);
            log.debug("提取到 DrawingML {} 图片: {}, ID: {}", type, (image.getFileName() != null ? image.getFileName() : "N/A"), image.getBlockId());
        } else {
            log.warn("未能通过ID '{}' 找到对应的图片数据 (DrawingML 类型: {}).", blipEmbedId, type);
        }
    }

    private Chapter createDefaultChapter(String docId, List<Chapter> chapters) {
        Heading defaultHeading = new Heading(1, "默认章节", "ch_" + docId.substring(0,Math.min(docId.length(),8)) + "_default_" + chapters.size());
        Chapter defaultChapter = new Chapter();
        defaultChapter.setChapterId(defaultHeading.getId());
        defaultChapter.setHeading(defaultHeading);
        defaultChapter.setSections(new ArrayList<>());
        defaultChapter.setDocumentId(docId);
        chapters.add(defaultChapter);
        log.debug("创建默认章节 ID: {}", defaultChapter.getChapterId());
        return defaultChapter;
    }

    private Section createDefaultSection(Chapter parentChapter, String docId, String title) {
        Heading defaultHeading = new Heading(2, title, "sec_" + docId.substring(0,Math.min(docId.length(),8)) + "_default_" + parentChapter.getChapterId() + "_" + parentChapter.getSections().size());
        Section defaultSection = new Section();
        defaultSection.setSectionId(defaultHeading.getId());
        defaultSection.setHeading(defaultHeading);
        defaultSection.setBlocks(new ArrayList<>());
        defaultSection.setChapterId(parentChapter.getChapterId());
        parentChapter.getSections().add(defaultSection);
        log.debug("创建默认节 ID: {} ('{}') 于章节 ID: {}", defaultSection.getSectionId(), title, parentChapter.getChapterId());
        return defaultSection;
    }

    private Section createDefaultSectionForSubHeading(Chapter parentChapter, Heading subHeadingAsSectionTitle, String docId) {
        // 使用子标题自身作为这个默认节的标题，但级别规范为2 (逻辑上的节标题)
        // 节的ID仍然使用传入的子标题的ID，因为它是这个"节"的唯一标识
        Heading sectionConceptualHeading = new Heading(2, subHeadingAsSectionTitle.getText(), subHeadingAsSectionTitle.getId());
        Section newSection = new Section();
        newSection.setSectionId(sectionConceptualHeading.getId()); // 关键：节的ID使用传入的子标题的ID
        newSection.setHeading(sectionConceptualHeading); // 节的标题用规范化的Heading对象
        newSection.setBlocks(new ArrayList<>());
        newSection.setChapterId(parentChapter.getChapterId());
        parentChapter.getSections().add(newSection);
        log.debug("为子标题 '{}' 创建内容归属的默认节 ID: {} 于章节 ID: {}", subHeadingAsSectionTitle.getText(), newSection.getSectionId(), parentChapter.getChapterId());
        return newSection;
    }

    private void assignBlockToStructure(Block block, Section currentSectionRef, Chapter currentChapterRef, String docId, List<Chapter> chaptersList, String originalFilename) {
        Section targetSection = currentSectionRef;
        Chapter owningChapter = currentChapterRef;

        if (targetSection != null) {
            block.setParentHeadingId(targetSection.getSectionId());
            block.setOrderInParent(targetSection.getBlocks().size());
            targetSection.getBlocks().add(block);
        } else if (owningChapter != null) {
            // 内容在章下面，但没有节，创建一个默认节来容纳它
            targetSection = createDefaultSection(owningChapter, docId, "默认内容节");
            block.setParentHeadingId(targetSection.getSectionId());
            block.setOrderInParent(targetSection.getBlocks().size()); // Should be 0 for new section
            targetSection.getBlocks().add(block);
            log.warn("块 ID: {} (类型: {}) 在章节 ID: {} 下，但无当前节，已创建默认节 ID: {} 来容纳。", block.getBlockId(), block.getType(), owningChapter.getChapterId(), targetSection.getSectionId());
        } else {
            // 没有章节也没有节 (通常是文档开头，或图片在任何标题之前)
            owningChapter = createDefaultChapter(docId, chaptersList);
            targetSection = createDefaultSection(owningChapter, docId, "默认文档内容");
            block.setParentHeadingId(targetSection.getSectionId());
            block.setOrderInParent(targetSection.getBlocks().size()); // Should be 0
            targetSection.getBlocks().add(block);
            log.warn("块 ID: {} (类型: {}) 出现时无任何章节结构，已创建默认章节 ID: {} 和默认节 ID: {} 来容纳。文档: {}", block.getBlockId(), block.getType(), owningChapter.getChapterId(), targetSection.getSectionId(), originalFilename);
        }
    }
} 