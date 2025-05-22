package com.ral.young.spring.ai.entity;

import cn.hutool.json.JSONObject;
import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.model.ModelRequest;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;

import java.util.List;

/**
 * @author renyh
 * @description 自定义嵌入请求参数
 * @date 2025/5/22 19:06
 * @since 1.0.0
 */
@Data
@Builder
public class CustomEmbeddingRequest implements ModelRequest<List<JSONObject>> {

	private List<JSONObject> inputs;

	private OpenAiEmbeddingOptions options;

	public CustomEmbeddingRequest() {
	}

	public CustomEmbeddingRequest(JSONObject... inputs) {
		this(Lists.newArrayList(inputs), OpenAiEmbeddingOptions.builder().build());
	}

	public CustomEmbeddingRequest(List<JSONObject> inputs, OpenAiEmbeddingOptions options) {
		this.inputs = inputs;
		this.options = options;
	}

	@Override
	public List<JSONObject> getInstructions() {
		return inputs;
	}
}
