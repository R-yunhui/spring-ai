package com.ral.young.spring.ai.entity;

import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.List;

/**
 * @param embeddings Embedding data.
 * @param metadata   Embedding metadata.
 * @author renyh
 * @description 定义返回结果
 * @date 2025/5/22 17:49
 * @since 1.0.0
 */
public record CustomEmbeddingResponse(List<CustomEmbedding> embeddings, EmbeddingResponseMetadata metadata) {

}
