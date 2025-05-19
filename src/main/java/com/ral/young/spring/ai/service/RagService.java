package com.ral.young.spring.ai.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.google.common.collect.Lists;
import com.ral.young.spring.ai.model.CustomEmbeddingModel;
import com.ral.young.spring.ai.model.Document;
import com.ral.young.spring.ai.model.dto.ChunkForEmbedding;
import com.ral.young.spring.ai.model.dto.ImageEmbeddingDTO;
import com.ral.young.spring.ai.model.dto.TextEmbeddingDTO;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.FieldType;
import io.milvus.param.dml.InsertParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.DocumentEmbeddingModel;
import org.springframework.ai.embedding.DocumentEmbeddingRequest;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * @author renyh
 * @description rag 的相关测试验证
 * @date 2025/5/12 11:01
 * @since 1.0.0
 */
@Slf4j
@Service
public class RagService {

	private final EmbeddingPreparationService embeddingPreparationService;

	private final WordParserService wordParserService;

	private final MilvusOperationService milvusOperationService;

	private final CustomEmbeddingModel embeddingModel;

	// Milvus Collection 和字段常量
	private static final String COLLECTION_NAME = "word_document_chunks";
	private static final int EMBEDDING_DIMENSION = 384; // 示例维度，请根据您的模型调整
	private static final String CHUNK_ID_FIELD = "chunk_id"; // 主键
	private static final String DOC_ID_FIELD = "doc_id";
	private static final String VECTOR_FIELD = "vector";
	private static final String TEXT_FIELD = "text";
	private static final String BLOCK_TYPE_FIELD = "block_type";
	private static final String HEADING_LEVEL_FIELD = "heading_level";
	private static final String HEADING_TEXT_FIELD = "heading_text";
	private static final String PARENT_ID_FIELD = "parent_id"; // 例如 parent_heading_id
	private static final String ORIGINAL_FILENAME_FIELD = "original_filename";

	// 索引参数
	private static final String INDEX_NAME = "idx_" + VECTOR_FIELD; // Milvus 2.x 索引名通常与字段名相关或自动
	private static final IndexType INDEX_TYPE = IndexType.HNSW; // 或者 IVF_FLAT, AUTOINDEX 等
	private static final MetricType METRIC_TYPE = MetricType.L2; // L2 距离或 IP (内积)
	private static final String INDEX_PARAMS_JSON = "{\"M\": 16, \"efConstruction\": 200}"; // HNSW 示例参数

	private static final Random RANDOM = new Random();
	private final CustomEmbeddingModel customEmbeddingModel;

	public RagService(EmbeddingPreparationService embeddingPreparationService, WordParserService wordParserService, MilvusOperationService milvusOperationService, CustomEmbeddingModel embeddingModel, CustomEmbeddingModel customEmbeddingModel) {
		this.embeddingPreparationService = embeddingPreparationService;
		this.wordParserService = wordParserService;
		this.milvusOperationService = milvusOperationService;
		this.embeddingModel = embeddingModel;
		this.customEmbeddingModel = customEmbeddingModel;
	}

	/**
	 * 在服务启动后初始化 Milvus 集合和索引。
	 */
	@PostConstruct
	public void initializeMilvus() {
		try {
			ensureMilvusCollectionAndIndex();
		} catch (Exception e) {
			log.error("RagService 初始化 Milvus 失败: {}", e.getMessage(), e);
			// 根据您的应用策略，这里可能需要抛出异常或者允许应用继续运行但功能受限
		}
	}

	/**
	 * 处理上传的 Word 文档，提取文本块，生成 Embedding (模拟)，并存储到 Milvus。
	 *
	 * @param file 用户上传的 Word (.docx) 文件
	 * @throws IOException 如果文档解析失败
	 */
	public void processAndEmbedDocument(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			log.warn("尝试处理的 Word 文件为空。");
			throw new IllegalArgumentException("上传的文件不能为空");
		}
		log.info("开始处理 Word 文档: {}", file.getOriginalFilename());

		// 1. 解析 Word 文档
		Document parsedDocument = wordParserService.parseWordDocument(file);
		if (parsedDocument == null) {
			log.error("文档解析失败: {}", file.getOriginalFilename());
			throw new IOException("文档解析返回 null: " + file.getOriginalFilename());
		}
		log.info("文档 {} 解析完成。DocID: {}", file.getOriginalFilename(), parsedDocument.getDocId());

		// 2. 准备用于 Embedding 的数据块
		List<ChunkForEmbedding> chunks = embeddingPreparationService.prepareChunks(parsedDocument, file.getOriginalFilename());
		if (chunks.isEmpty()) {
			log.warn("从文档 {} 未提取到任何数据块用于 Embedding。", parsedDocument.getDocId());
			return;
		}
		log.info("从文档 {} 提取到 {} 个数据块用于 Embedding。", parsedDocument.getDocId(), chunks.size());

		// 3. 为每个数据块生成 Embedding (模拟) 并准备存入 Milvus
		List<InsertParam.Field> fieldsDataList = new ArrayList<>();
		List<String> chunkIds = new ArrayList<>();
		List<String> docIds = new ArrayList<>();
		List<List<Float>> vectors = new ArrayList<>();
		List<String> texts = new ArrayList<>();
		List<String> blockTypes = new ArrayList<>();
		List<Integer> headingLevels = new ArrayList<>();
		List<String> headingTexts = new ArrayList<>();
		List<String> parentIds = new ArrayList<>();
		List<String> originalFilenames = new ArrayList<>();

		int chunksProcessed = 0;
		for (ChunkForEmbedding chunk : chunks) {
			if (chunk.getChunkId() == null || chunk.getChunkText() == null || chunk.getChunkText().trim().isEmpty()) {
				log.warn("跳过无效的数据块 (ChunkID: {}, ChunkText empty: {}).", 
						 chunk.getChunkId(), 
						 chunk.getChunkText() == null || chunk.getChunkText().trim().isEmpty());
				continue;
			}

			// 模拟 Embedding 生成
			List<Float> vector = generateSimulatedEmbedding(new TextEmbeddingDTO(chunk.getChunkText()));

			chunkIds.add(chunk.getChunkId());
			docIds.add(chunk.getDocumentId());
			vectors.add(vector);
			texts.add(chunk.getChunkText());
			originalFilenames.add(chunk.getOriginalFilename());

			// 从元数据提取字段，提供默认值
			Map<String, Object> metadata = chunk.getMetadata();
			blockTypes.add(Objects.toString(metadata.get("block_type"), "N/A"));
			headingLevels.add((Integer) metadata.getOrDefault("heading_level", 0));
			headingTexts.add(Objects.toString(metadata.get("heading_text"), ""));
			parentIds.add(Objects.toString(metadata.get("parent_id"), ""));
			
			chunksProcessed++;
		}
		
		if (chunksProcessed == 0) {
			log.warn("没有有效的数据块可供存入 Milvus (文档: {})。", parsedDocument.getDocId());
			return;
		}

		fieldsDataList.add(new InsertParam.Field(CHUNK_ID_FIELD, chunkIds));
		fieldsDataList.add(new InsertParam.Field(DOC_ID_FIELD, docIds));
		fieldsDataList.add(new InsertParam.Field(VECTOR_FIELD, vectors));
		fieldsDataList.add(new InsertParam.Field(TEXT_FIELD, texts));
		fieldsDataList.add(new InsertParam.Field(BLOCK_TYPE_FIELD, blockTypes));
		fieldsDataList.add(new InsertParam.Field(HEADING_LEVEL_FIELD, headingLevels));
		fieldsDataList.add(new InsertParam.Field(HEADING_TEXT_FIELD, headingTexts));
		fieldsDataList.add(new InsertParam.Field(PARENT_ID_FIELD, parentIds));
		fieldsDataList.add(new InsertParam.Field(ORIGINAL_FILENAME_FIELD, originalFilenames));

		// 4. 存储到 Milvus (使用 Upsert)
		log.info("准备将 {} 个数据块 Upsert 到 Milvus 集合 {}", chunksProcessed, COLLECTION_NAME);
		try {
			MutationResult upsertResult = milvusOperationService.upsert(COLLECTION_NAME, null, fieldsDataList);
			if (upsertResult != null && upsertResult.getUpsertCnt() > 0) {
				log.info("成功 Upsert {} 条数据到 Milvus。 Inserted: {}, Updated: {} (approximated by upsertCnt)",
						upsertResult.getUpsertCnt(), upsertResult.getInsertCnt(), upsertResult.getUpsertCnt() - upsertResult.getInsertCnt());
				 if (upsertResult.getSuccIndexCount() > 0) {
					 log.debug("成功 Upsert 的主键数量: {}", upsertResult.getSuccIndexCount());
				 }
				 if (upsertResult.getErrIndexCount() > 0) {
					 log.warn("Upsert 过程中出现 {} 个错误。", upsertResult.getErrIndexCount());
				 }
			} else {
				log.warn("Upsert 操作未成功执行或未影响任何行。Result: {}", upsertResult);
			}
		} catch (Exception e) {
			log.error("将数据存储到 Milvus 失败 (文档 {}): {}", parsedDocument.getDocId(), e.getMessage(), e);
			// 可以在这里抛出自定义异常或进行重试等错误处理
		}
		log.info("文档 {} 处理和 Embedding 存储流程完成。", file.getOriginalFilename());
	}

	/**
	 * 确保 Milvus 中存在所需的集合和索引。
	 * 如果不存在，则尝试创建它们。
	 */
	private void ensureMilvusCollectionAndIndex() {
		log.info("开始检查并初始化 Milvus 集合 {}...", COLLECTION_NAME);
		try {
			if (!milvusOperationService.hasCollection(COLLECTION_NAME)) {
				log.info("Milvus 集合 {} 不存在，开始创建...", COLLECTION_NAME);

				List<FieldType> otherFields = new ArrayList<>();
				otherFields.add(FieldType.newBuilder().withName(DOC_ID_FIELD).withDataType(DataType.VarChar).withMaxLength(255).build());
				// 注意：Milvus VARCHAR 类型的最大长度为 65535 字节。如果文本很长，请考虑分块或摘要。
				otherFields.add(FieldType.newBuilder().withName(TEXT_FIELD).withDataType(DataType.VarChar).withMaxLength(65530).build()); // 稍小于最大值以防万一
				otherFields.add(FieldType.newBuilder().withName(BLOCK_TYPE_FIELD).withDataType(DataType.VarChar).withMaxLength(50).build());
				otherFields.add(FieldType.newBuilder().withName(HEADING_LEVEL_FIELD).withDataType(DataType.Int32).build());
				otherFields.add(FieldType.newBuilder().withName(HEADING_TEXT_FIELD).withDataType(DataType.VarChar).withMaxLength(1024).build());
				otherFields.add(FieldType.newBuilder().withName(PARENT_ID_FIELD).withDataType(DataType.VarChar).withMaxLength(255).build());
				otherFields.add(FieldType.newBuilder().withName(ORIGINAL_FILENAME_FIELD).withDataType(DataType.VarChar).withMaxLength(512).build());

				boolean collectionCreated = milvusOperationService.createCollection(
						COLLECTION_NAME,
						"Collection to store chunks from Word documents for RAG",
						CHUNK_ID_FIELD, // 主键字段名称
						VECTOR_FIELD,   // 向量字段名称
						EMBEDDING_DIMENSION,
						otherFields
				);

				if (collectionCreated) {
					log.info("Milvus 集合 {} 创建成功。", COLLECTION_NAME);
					// 为向量字段创建索引
					log.info("开始为集合 {} 的字段 {} 创建索引...", COLLECTION_NAME, VECTOR_FIELD);
					boolean indexCreated = milvusOperationService.createIndex(
							COLLECTION_NAME,
							VECTOR_FIELD,
							INDEX_NAME, // Milvus 2.x SDK 好像 indexName 作用不大，但有些版本需要
							INDEX_TYPE,
							METRIC_TYPE,
							INDEX_PARAMS_JSON
					);
					if (indexCreated) {
						log.info("集合 {} 的索引 {} 创建请求已提交。等待索引就绪...", COLLECTION_NAME, INDEX_NAME);
						// 等待索引构建完成 (根据 MilvusOperationService 的实现，它可能是异步的)
						// 实际项目中，这里可能需要一个轮询或回调机制
						// 为简单起见，这里假设 createIndex 之后可以立即加载或 MilvusOperationService 内部处理了等待
						milvusOperationService.waitForIndexReady(COLLECTION_NAME, VECTOR_FIELD, 300, 1000);

					} else {
						log.error("为集合 {} 的字段 {} 创建索引失败。", COLLECTION_NAME, VECTOR_FIELD);
					}
				} else {
					log.error("Milvus 集合 {} 创建失败。", COLLECTION_NAME);
					return; // 创建失败则不继续
				}
			} else {
				log.info("Milvus 集合 {} 已存在。", COLLECTION_NAME);
				// 可以考虑检查索引是否存在并按需创建，但为简化，此处假设存在集合即索引也配置妥当
				// 或者，可以调用 describeIndex 和 getIndexState 来确认
			}

			// 加载集合到内存
			log.info("开始加载 Milvus 集合 {} 到内存...", COLLECTION_NAME);
			if (milvusOperationService.loadCollection(COLLECTION_NAME)) {
				log.info("Milvus 集合 {} 加载成功或已加载。", COLLECTION_NAME);
			} else {
				log.warn("Milvus 集合 {} 加载失败。", COLLECTION_NAME);
			}

		} catch (Exception e) {
			log.error("初始化 Milvus 集合 {} 或索引时发生严重错误: {}", COLLECTION_NAME, e.getMessage(), e);
			// 抛出运行时异常，指示服务可能无法正常工作
			throw new RuntimeException("Milvus 初始化失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 生成指定维度的模拟浮点型 Embedding 向量。
	 * 实际应用中，这里应该调用真正的 Embedding 模型服务。
	 *
	 * @return 随机生成的向量列表
	 */
	public List<Float> generateSimulatedEmbedding(TextEmbeddingDTO text) {
		Embedding result = embeddingModel.call(new EmbeddingRequest(Lists.newArrayList(JSONUtil.toJsonStr(text)), customEmbeddingModel.getDefaultOptions())).getResult();
		float[] output = result.getOutput();
		List<Float> floatList = new ArrayList<>();
		// 手动遍历数组并添加元素到列表
		for (float value : output) {
			floatList.add(value);
		}
		return floatList;
	}

	public List<JSONObject> generateSimulatedEmbeddingTwo(List<TextEmbeddingDTO> textEmbeddingDTOList) {
		return embeddingModel.call2(textEmbeddingDTOList);
	}

	public List<JSONObject> generateSimulatedEmbeddingThree(List<ImageEmbeddingDTO> imageEmbeddingDTOList) {
		return embeddingModel.call2(imageEmbeddingDTOList);
	}
}
