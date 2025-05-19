package com.ral.young.spring.ai.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import com.ral.young.spring.ai.model.Document;
import com.ral.young.spring.ai.model.dto.ChunkForEmbedding;
import com.ral.young.spring.ai.model.dto.ImageEmbeddingDTO;
import com.ral.young.spring.ai.model.dto.TextEmbeddingDTO;
import com.ral.young.spring.ai.service.EmbeddingPreparationService;
import com.ral.young.spring.ai.service.RagService;
import com.ral.young.spring.ai.service.WordParserService;
import io.milvus.param.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Word 文档解析控制器。
 * 提供 HTTP 接口用于上传和解析 Word (.docx) 文档。
 */
@RestController
@RequestMapping("/api/ai/word")
@Slf4j
public class WordParserController {

	private final WordParserService wordParserService;

	private final EmbeddingPreparationService embeddingPreparationService;

	private final RagService ragService;

	public WordParserController(WordParserService wordParserService, EmbeddingPreparationService embeddingPreparationService, RagService ragService) {
		this.wordParserService = wordParserService;
		this.embeddingPreparationService = embeddingPreparationService;
		this.ragService = ragService;
	}


	/**
	 * 生成模拟的 Embedding 向量。
	 *
	 * @param textList 输入的文本内容
	 * @return 包含模拟 Embedding 向量的响应实体
	 */
	@PostMapping("/generate-simulated-embedding-two")
	public ResponseEntity<List<JSONObject>> generateSimulatedEmbeddingTwo(@RequestBody List<TextEmbeddingDTO> textList) {
		log.info("接收到生成模拟 Embedding 向量的请求，输入文本: {}", textList);
		try {
			// 调用 RagService 中的方法生成模拟 Embedding 向量
			List<JSONObject> embedding = ragService.generateSimulatedEmbeddingTwo(textList);
			return ResponseEntity.ok(embedding);
		} catch (Exception e) {
			log.error("生成模拟 Embedding 向量时发生错误: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}


	/**
	 * 生成模拟的 Embedding 向量。
	 *
	 * @param imgList 输入的文本内容
	 * @return 包含模拟 Embedding 向量的响应实体
	 */
	@PostMapping("/generate-simulated-embedding-three")
	public ResponseEntity<List<JSONObject>> generateSimulatedEmbeddingThree(@RequestBody List<ImageEmbeddingDTO> imgList) {
		log.info("接收到生成模拟 Embedding 向量的请求，输入图片: {}", imgList);
		try {
			// 调用 RagService 中的方法生成模拟 Embedding 向量
			List<JSONObject> embedding = ragService.generateSimulatedEmbeddingThree(imgList);
			return ResponseEntity.ok(embedding);
		} catch (Exception e) {
			log.error("生成模拟 Embedding 向量时发生错误: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	/**
	 * 解析上传的 Word (.docx) 文档文件。
	 *
	 * @param file 用户上传的 .docx 文件，通过 multipart/form-data 方式提交。
	 *             请求参数名应为 "file"。
	 * @return 如果解析成功，返回包含结构化文档内容的 {@link Document} 对象和 HTTP 200 (OK) 状态。
	 * 如果文件为空、格式错误或解析过程中发生 I/O 错误，将返回相应的错误信息和 HTTP 状态码。
	 */
	@PostMapping("/parse")
	public ResponseEntity<?> parseWordDocument(@RequestParam("file") MultipartFile file) {
		log.info("接收到 Word 文档解析请求，文件名: {}", file.getOriginalFilename());
		if (file.isEmpty()) {
			log.warn("上传的文件为空: {}", file.getOriginalFilename());
			return ResponseEntity.badRequest().body("错误：上传的文件不能为空。");
		}
		if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase().endsWith(".docx")) {
			log.warn("上传的文件格式不正确: {}，仅支持 .docx", file.getOriginalFilename());
			return ResponseEntity.badRequest().body("错误：文件格式不正确，请上传 .docx 文件。");
		}

		try {
			Document parsedDocument = wordParserService.parseWordDocument(file);
			log.info("Word 文档 {} 解析成功。返回结构化数据。", file.getOriginalFilename());
			List<ChunkForEmbedding> chunkForEmbeddings = new ArrayList<>();
			if (ObjectUtil.isNotNull(parsedDocument)) {
				chunkForEmbeddings = embeddingPreparationService.prepareChunks(parsedDocument, file.getOriginalFilename());
			}

			return ResponseEntity.ok(chunkForEmbeddings);
		} catch (IllegalArgumentException e) {
			log.error("解析 Word 文档 {} 时发生参数错误: {}", file.getOriginalFilename(), e.getMessage());
			return ResponseEntity.badRequest().body("解析错误: " + e.getMessage());
		} catch (IOException e) {
			log.error("解析 Word 文档 {} 时发生 I/O 错误: {}", file.getOriginalFilename(), e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("服务器内部错误：解析文档时发生 I/O 问题。");
		} catch (Exception e) {
			log.error("解析 Word 文档 {} 时发生未知错误: {}", file.getOriginalFilename(), e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("服务器内部错误：解析过程中发生意外错误。");
		}
	}
} 