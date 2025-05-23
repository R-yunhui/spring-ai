package com.ral.young.spring.ai.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.common.OpenAiApiConstants;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Consumer;

/**
 * @author renyh
 * @description 自定义 OpenAi Api 实现
 * @date 2025/5/22 17:22
 * @since 1.0.0
 */
public class CustomOpenAiApi extends OpenAiApi {

	public static Builder builder(ApiKey apiKey) {
		return new Builder().apiKey(apiKey);
	}

	private final RestClient restClient;

	private final String embeddingsPath;

	public CustomOpenAiApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String> headers, String completionsPath, String embeddingsPath, RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		super(baseUrl, apiKey, headers, completionsPath, embeddingsPath, restClientBuilder, webClientBuilder, responseErrorHandler);
		Consumer<HttpHeaders> finalHeaders = h -> {
			if(!(apiKey instanceof NoopApiKey)) {
				h.setBearerAuth(apiKey.getValue());
			}

			h.setContentType(MediaType.APPLICATION_JSON);
			h.addAll(headers);
		};
		this.restClient = restClientBuilder.baseUrl(baseUrl)
				.defaultHeaders(finalHeaders)
				.defaultStatusHandler(responseErrorHandler)
				.build();
		this.embeddingsPath = embeddingsPath;
	}

	@Override
	public <T> ResponseEntity<EmbeddingList<Embedding>> embeddings(EmbeddingRequest<T> embeddingRequest) {
		return this.restClient.post()
				.uri(this.embeddingsPath)
				.body(embeddingRequest)
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {

				});
	}

	public <T> ResponseEntity<EmbeddingList<CurEmbedding>> customEmbeddings(EmbeddingRequest<T> embeddingRequest) {
		return this.restClient.post()
				.uri(this.embeddingsPath)
				.body(embeddingRequest)
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {

				});
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CurEmbedding(// @formatter:off
							@JsonProperty("index") Integer index,
							@JsonProperty("embedding") double[] embedding,
							@JsonProperty("object") String object) { // @formatter:on


		public CurEmbedding(Integer index, double[] embedding) {
			this(index, embedding, "embedding");
		}

	}

	public static class Builder {

		private String baseUrl = OpenAiApiConstants.DEFAULT_BASE_URL;

		private ApiKey apiKey;

		private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		private String completionsPath = "/v1/chat/completions";

		private String embeddingsPath = "/v1/embeddings";

		private RestClient.Builder restClientBuilder = RestClient.builder();

		private WebClient.Builder webClientBuilder = WebClient.builder();

		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		public Builder baseUrl(String baseUrl) {
			Assert.hasText(baseUrl, "baseUrl cannot be null or empty");
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder apiKey(ApiKey apiKey) {
			Assert.notNull(apiKey, "apiKey cannot be null");
			this.apiKey = apiKey;
			return this;
		}

		public Builder apiKey(String simpleApiKey) {
			Assert.notNull(simpleApiKey, "simpleApiKey cannot be null");
			this.apiKey = new SimpleApiKey(simpleApiKey);
			return this;
		}

		public Builder headers(MultiValueMap<String, String> headers) {
			Assert.notNull(headers, "headers cannot be null");
			this.headers = headers;
			return this;
		}

		public Builder completionsPath(String completionsPath) {
			Assert.hasText(completionsPath, "completionsPath cannot be null or empty");
			this.completionsPath = completionsPath;
			return this;
		}

		public Builder embeddingsPath(String embeddingsPath) {
			Assert.hasText(embeddingsPath, "embeddingsPath cannot be null or empty");
			this.embeddingsPath = embeddingsPath;
			return this;
		}

		public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
			Assert.notNull(restClientBuilder, "restClientBuilder cannot be null");
			this.restClientBuilder = restClientBuilder;
			return this;
		}

		public Builder webClientBuilder(WebClient.Builder webClientBuilder) {
			Assert.notNull(webClientBuilder, "webClientBuilder cannot be null");
			this.webClientBuilder = webClientBuilder;
			return this;
		}

		public Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
			Assert.notNull(responseErrorHandler, "responseErrorHandler cannot be null");
			this.responseErrorHandler = responseErrorHandler;
			return this;
		}

		public CustomOpenAiApi build() {
			Assert.notNull(this.apiKey, "apiKey must be set");
			return new CustomOpenAiApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath, this.embeddingsPath,
					this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
		}

	}
}
