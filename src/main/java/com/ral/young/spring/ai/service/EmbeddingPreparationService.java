package com.ral.young.spring.ai.service;

import com.ral.young.spring.ai.model.dto.ChunkForEmbedding; // 确保这个路径正确
import com.ral.young.spring.ai.model.Chapter;
import com.ral.young.spring.ai.model.Document;
import com.ral.young.spring.ai.model.Section;
import com.ral.young.spring.ai.model.block.Block;
import com.ral.young.spring.ai.model.block.Image;
import com.ral.young.spring.ai.model.block.Paragraph;
import com.ral.young.spring.ai.model.block.Table;
import com.ral.young.spring.ai.model.block.TableRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 负责将解析后的 {@link Document} 对象转换为适合进行Embedding的文本片段列表。
 * 每个文本片段将包含上下文信息（如章节、节标题）和详细的元数据。
 */
@Service
@Slf4j
public class EmbeddingPreparationService {

	// 匹配常见的中文和英文句子结束符，用于简单的句子分割
	// 注意：此正则表达式对于复杂文本可能不够鲁棒，建议在生产环境中使用更专业的NLP库进行句子分割。
	private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("(?<=[。！？?!.])\\s*");

	@Value("${embedding.chunk.max-char-length:500}") // 可以通过 application.properties 配置
	private int maxChunkCharLength;

	@Value("${embedding.chunk.overlap-sentences:1}") // 可以通过 application.properties 配置
	private int overlapSentencesCount;

	/**
	 * 准备文档中所有内容的Embedding片段。
	 *
	 * @param document         解析后的 {@link Document} 对象。
	 * @param originalFilename 原始上传的文件名。
	 * @return 一个包含所有待Embedding文本片段 ({@link ChunkForEmbedding}) 的列表。
	 */
	public List<ChunkForEmbedding> prepareChunks(Document document, String originalFilename) {
		List<ChunkForEmbedding> chunks = new ArrayList<>();
		if (document == null) {
			log.warn("输入文档对象为 null，无法准备Embedding片段。原始文件名: {}", originalFilename);
			return chunks;
		}
		if (document.getChapters() == null || document.getChapters().isEmpty()) {
			log.warn("文档对象不包含任何章节，无法准备Embedding片段。文档ID: {}, 原始文件名: {}", document.getDocId(), originalFilename);
			return chunks;
		}

		String docId = document.getDocId();
		log.info("开始为文档 ID: {} (文件名: {}) 准备Embedding片段。最大片段长度: {}字符, 重叠句子数: {}",
				docId, originalFilename, maxChunkCharLength, overlapSentencesCount);

		for (Chapter chapter : document.getChapters()) {
			String chapterTitle = (chapter.getHeading() != null && StringUtils.hasText(chapter.getHeading().getText()))
					? chapter.getHeading().getText().trim() : "无章节标题";
			String chapterId = chapter.getChapterId();

			// 章节不直接包含块，块在节(Section)中
			if (chapter.getSections() != null && !chapter.getSections().isEmpty()) {
				for (Section section : chapter.getSections()) {
					String sectionTitle = (section.getHeading() != null && StringUtils.hasText(section.getHeading().getText()))
							? section.getHeading().getText().trim() : "无节标题";
					String sectionId = section.getSectionId();
					log.debug("处理章节 '{}' (ID: {}) -> 节 '{}' (ID: {}) 下的块...",
							chapterTitle, chapterId, sectionTitle, sectionId);

					if (section.getBlocks() != null && !section.getBlocks().isEmpty()) {
						for (Block block : section.getBlocks()) {
							processBlockAndAddChunks(block, docId, originalFilename,
									chapterId, chapterTitle,
									sectionId, sectionTitle,
									chunks);
						}
					} else {
						log.debug("节 '{}' (ID: {}) 不包含任何块。", sectionTitle, sectionId);
					}
				}
			} else {
				log.debug("章节 '{}' (ID: {}) 不包含任何节。", chapterTitle, chapterId);
			}
		}
		log.info("文档 ID: {} (文件名: {}) 的Embedding片段准备完成，共生成 {} 个片段。", docId, originalFilename, chunks.size());
		return chunks;
	}

	/**
	 * 处理单个块元素，将其转换为一个或多个 {@link ChunkForEmbedding} 对象，并添加到结果列表中。
	 *
	 * @param block            要处理的 {@link Block} 对象。
	 * @param docId            文档ID。
	 * @param originalFilename 原始文件名。
	 * @param chapterId        所属章节ID。
	 * @param chapterTitle     所属章节标题。
	 * @param sectionId        所属节ID (可能为null，如果块直接隶属于章节或默认结构)。
	 * @param sectionTitle     所属节标题 (可能为null)。
	 * @param allChunksList    用于收集所有文档生成的Embedding片段的总列表。
	 */
	private void processBlockAndAddChunks(Block block, String docId, String originalFilename,
										  String chapterId, String chapterTitle,
										  String sectionId, String sectionTitle,
										  List<ChunkForEmbedding> allChunksList) {

		AtomicInteger intraBlockChunkCounter = new AtomicInteger(0); // 用于在单个block内部因分割产生多个chunk时进行编号
		String contextPrefix = buildContextPrefix(chapterTitle, sectionTitle);
		String blockId = block.getBlockId();

		Map<String, Object> baseMetadata = new HashMap<>();
		baseMetadata.put("document_id", docId);
		baseMetadata.put("original_filename", originalFilename);
		baseMetadata.put("chapter_id", chapterId);
		baseMetadata.put("chapter_title", chapterTitle);
		// 只有当节信息有效且不是默认的“无节标题”时才加入，避免误导
		if (StringUtils.hasText(sectionId) && StringUtils.hasText(sectionTitle) && !"无节标题".equals(sectionTitle)) {
			baseMetadata.put("section_id", sectionId);
			baseMetadata.put("section_title", sectionTitle);
		}
		baseMetadata.put("block_id", blockId);
		baseMetadata.put("block_type", block.getType().name());
		baseMetadata.put("order_in_parent", block.getOrderInParent());

		log.trace("准备处理块 ID: {}, 类型: {}", blockId, block.getType());

		String contentToProcess = "";
		String typeSpecificPrefix = "";

		switch (block.getType()) {
			case PARAGRAPH:
				Paragraph p = (Paragraph) block;
				contentToProcess = p.getText();
				break;

			case TABLE:
				Table t = (Table) block;
				contentToProcess = convertTableToMarkdown(t);
				typeSpecificPrefix = "表格内容：\n";
				// 为表格块特有的元数据创建一个新的Map副本，或添加到baseMetadata后确保在其他类型处理前被清除/覆盖
				Map<String, Object> tableMeta = new HashMap<>(baseMetadata);
				tableMeta.put("table_row_count", t.getRowCount());
				tableMeta.put("table_column_count", t.getColumnCount());
				baseMetadata = tableMeta; // 后续子块将继承这些表格特定元数据
				break;

			case IMAGE:
				Image img = (Image) block;
				contentToProcess = img.getDescription();
				typeSpecificPrefix = "图片描述：";
				Map<String, Object> imageMeta = new HashMap<>(baseMetadata);
				if (StringUtils.hasText(img.getFileName())) {
					imageMeta.put("image_filename", img.getFileName());
				}
				baseMetadata = imageMeta; // 后续子块将继承这些图片特定元数据
				break;

			default:
				log.warn("遇到未明确处理的Block类型: {} (ID: {})，该块将被忽略。", block.getType(), blockId);
				return;
		}

		if (StringUtils.hasText(contentToProcess)) {
			List<String> subChunkTexts = splitTextIfNecessary(contentToProcess, maxChunkCharLength, overlapSentencesCount);
			for (String subText : subChunkTexts) {
				if (StringUtils.hasText(subText)) {
					String chunkId = generateChunkId(docId, blockId, intraBlockChunkCounter.getAndIncrement());
					String finalText = contextPrefix + typeSpecificPrefix + subText.trim(); // 确保subText也被trim

					// 每次为chunk创建一个新的元数据副本，以防baseMetadata被后续特定类型修改污染
					Map<String, Object> metadataForThisChunk = new HashMap<>(baseMetadata);
					if (block.getType() == com.ral.young.spring.ai.model.enums.BlockType.PARAGRAPH) {
						// 只有段落块才添加原始内容预览，避免表格和图片描述的元数据中也出现这个
						assert block instanceof Paragraph;
						String originalParagraphText = ((Paragraph) block).getText();
						metadataForThisChunk.put("original_content_preview", originalParagraphText.substring(0, Math.min(originalParagraphText.length(), 100)) + (originalParagraphText.length() > 100 ? "..." : ""));
					}

					allChunksList.add(new ChunkForEmbedding(chunkId, docId, originalFilename, finalText, metadataForThisChunk));
					log.trace("生成文本片段: ChunkID={}, 原始块类型={}, 长度={}", chunkId, block.getType().name(), finalText.length());
				}
			}
		} else {
			log.trace("块 ID: {} (类型: {}) 内容为空或处理后为空，跳过生成片段。", blockId, block.getType());
		}
	}

	/**
	 * 构建用于Embedding的文本片段的上下文前缀。
	 *
	 * @param chapterTitle 章节标题。
	 * @param sectionTitle 节标题 (可能为null或"无节标题")。
	 * @return 包含章节和节信息的文本前缀。
	 */
	private String buildContextPrefix(String chapterTitle, String sectionTitle) {
		StringBuilder prefix = new StringBuilder();
		if (StringUtils.hasText(chapterTitle) && !"无章节标题".equals(chapterTitle)) {
			prefix.append("章节标题：").append(chapterTitle).append("\n");
		}
		if (StringUtils.hasText(sectionTitle) && !"无节标题".equals(sectionTitle)) {
			prefix.append("节标题：").append(sectionTitle).append("\n");
		}
		return prefix.toString();
	}

	/**
	 * 生成文本片段的唯一ID。
	 *
	 * @param docId      文档ID。
	 * @param blockId    块ID。
	 * @param chunkIndex 该块内部分割出的子片段的索引。
	 * @return 唯一的片段ID。
	 */
	private String generateChunkId(String docId, String blockId, int chunkIndex) {
		return String.format("%s_%s_chunk_%03d", docId, blockId, chunkIndex);
	}

	/**
	 * 将表格对象转换为Markdown格式的文本表示。
	 *
	 * @param table 要转换的 {@link Table} 对象。
	 * @return 表格的Markdown字符串表示；如果表格为空或无法转换，则返回空字符串。
	 */
	private String convertTableToMarkdown(Table table) {
		if (table == null || table.getRows() == null || table.getRows().isEmpty()) {
			log.trace("尝试转换的表格 (Block ID: {}) 为空或无行数据。", table != null ? table.getBlockId() : "N/A");
			return "";
		}
		StringBuilder sb = new StringBuilder();
		List<TableRow> rows = table.getRows();

		boolean headerProcessed = false;
		int determinedColumnCount = 0;

		for (TableRow row : rows) {
			if (row.getCells() != null) {
				determinedColumnCount = Math.max(determinedColumnCount, row.getCells().size());
			}
		}
		if (determinedColumnCount == 0) {
			log.warn("表格 (Block ID: {}) 所有行均无单元格数据或列数为0，无法转换为Markdown。", table.getBlockId());
			return "";
		}

		for (TableRow row : rows) {
			// 确保行和单元格列表不是null
			if (row != null && row.getCells() != null && !row.getCells().isEmpty()) {
				sb.append("| ");
				for (int k=0; k < determinedColumnCount; k++) {
					String cellText = (k < row.getCells().size() && row.getCells().get(k) != null)
							? row.getCells().get(k).replace("\n", " ").replace("\r", " ").replace("|", "\\|").trim() // 转义|
							: "";
					sb.append(cellText).append(" | ");
				}
				sb.append("\n");

				if (!headerProcessed && row.isHeaderRow()) {
					sb.append("|");
					sb.append("---|".repeat(determinedColumnCount));
					sb.append("\n");
					headerProcessed = true;
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 如果文本超过指定的最大长度，则尝试按句子进行分割。
	 * 分割后的片段会考虑一定的句子重叠。
	 * <p>
	 * <b>注意：</b> 这是一个基于正则表达式的简化句子分割实现，对于复杂的、
	 * 专业领域的文本，其准确性可能有限。在生产环境中，建议使用更成熟的NLP库。
	 * </p>
	 *
	 * @param textToSplit           要分割的原始文本。
	 * @param maxChars              每个片段的最大字符长度限制。
	 * @param overlapSentencesNum   相邻片段之间重叠的句子数量。
	 * @return 分割后的文本片段列表。如果文本未超长或无法有效分割，则列表只包含原始文本。
	 */
	private List<String> splitTextIfNecessary(String textToSplit, int maxChars, int overlapSentencesNum) {
		List<String> resultingChunks = new ArrayList<>();
		if (!StringUtils.hasText(textToSplit)) {
			return resultingChunks;
		}
		String trimmedText = textToSplit.trim();

		if (trimmedText.length() <= maxChars) {
			resultingChunks.add(trimmedText);
			return resultingChunks;
		}

		List<String> sentences = Arrays.stream(SENTENCE_SPLIT_PATTERN.split(trimmedText))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.toList();

		if (sentences.isEmpty()) {
			log.warn("文本(长度 {})使用正则分割后未产生有效句子，将尝试按最大长度硬分割。", trimmedText.length());
			for (int i = 0; i < trimmedText.length(); i += maxChars) {
				resultingChunks.add(trimmedText.substring(i, Math.min(trimmedText.length(), i + maxChars)));
			}
			return resultingChunks.stream().filter(StringUtils::hasText).collect(Collectors.toList());
		}
		if (sentences.size() == 1) {
			log.warn("文本长度 {} 超过最大限制 {}，但只有一个句子。将按最大长度硬分割此句子。", trimmedText.length(), maxChars);
			String singleSentence = sentences.getFirst();
			for (int i = 0; i < singleSentence.length(); i += maxChars) {
				resultingChunks.add(singleSentence.substring(i, Math.min(singleSentence.length(), i + maxChars)));
			}
			return resultingChunks.stream().filter(StringUtils::hasText).collect(Collectors.toList());
		}

		List<String> currentChunkSentences = new ArrayList<>();
		int currentChunkLength = 0;

		for (int i = 0; i < sentences.size(); i++) {
			String sentence = sentences.get(i);
			int sentenceLength = sentence.length();
			// 预估加入当前句子后的长度 (加上空格)
			int estimatedLengthWithNewSentence = currentChunkLength + sentenceLength + (currentChunkSentences.isEmpty() ? 0 : 1);

			if (currentChunkSentences.isEmpty() || estimatedLengthWithNewSentence <= maxChars) {
				currentChunkSentences.add(sentence);
				currentChunkLength = String.join(" ", currentChunkSentences).length();
			} else {
				// 当前块已满，提交当前块
				resultingChunks.add(String.join(" ", currentChunkSentences));

				// 开始新块，并处理重叠
				List<String> sentencesForNewChunk = new ArrayList<>();
				// 从上一个块的末尾选取重叠句子，或者从当前句子的前几句开始
				// 我们希望重叠的是上一个块的末尾部分句子
				int overlapLookBackStart = Math.max(0, currentChunkSentences.size() - overlapSentencesNum);
				if (!currentChunkSentences.isEmpty() && overlapSentencesNum > 0) { // 从上一个提交的块中取重叠
					for(int k=overlapLookBackStart; k < currentChunkSentences.size(); k++) {
						sentencesForNewChunk.add(currentChunkSentences.get(k));
					}
				} else if (overlapSentencesNum > 0) { // 如果上一个块是空的（不太可能到这里），从当前句子往前看
					for (int k = Math.max(0, i - overlapSentencesNum); k < i; k++) {
						sentencesForNewChunk.add(sentences.get(k));
					}
				}


				currentChunkSentences.clear();
				currentChunkSentences.addAll(sentencesForNewChunk);
				currentChunkLength = String.join(" ", currentChunkSentences).length();


				// 将当前句子加入新块 (如果还能放下)
				if (currentChunkLength + sentenceLength + (currentChunkSentences.isEmpty() ? 0 : 1) <= maxChars || currentChunkSentences.isEmpty()) {
					currentChunkSentences.add(sentence);
					currentChunkLength = String.join(" ", currentChunkSentences).length();
				} else {
					// 如果当前句子单独也超长（即使在新块中），则先提交已有的重叠部分（如果有）
					resultingChunks.add(String.join(" ", currentChunkSentences));
					// 再将这个超长句子单独处理（可能硬分割）
					log.warn("句子 '{}' (长度 {}) 单独处理，因其可能使块超长 (限制 {}).", sentence, sentenceLength, maxChars);
					for (int k = 0; k < sentenceLength; k += maxChars) {
						resultingChunks.add(sentence.substring(k, Math.min(sentenceLength, k + maxChars)));
					}
					currentChunkSentences.clear(); // 这个长句子已经处理完了
					currentChunkLength = 0;
				}
			}
		}

		if (!currentChunkSentences.isEmpty()) {
			resultingChunks.add(String.join(" ", currentChunkSentences));
		}

		return resultingChunks.stream().map(String::trim).filter(StringUtils::hasText).collect(Collectors.toList());
	}
}