package com.ral.young.spring.ai.entity;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;

import java.util.List;

/**
 * @author renyh
 * @description 自定义嵌入请求参数
 * @date 2025/5/22 19:06
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomEmbeddingRequest {

	private List<JSONObject> inputs;

	private OpenAiEmbeddingOptions options;
}
