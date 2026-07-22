package com.company.assistant.chatbot;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;


@Component
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String model;

    public OllamaEmbeddingClient(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.embedding-model}") String model) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
    }

    @Override
    public float[] embed(String text) {
        EmbedResponse response = restClient.post()
                .uri("/api/embed")
                .body(new EmbedRequest(model, text, -1))
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new IllegalStateException("Ollama bos embedding dondu: " + text);
        }

        List<Float> vector = response.embeddings().get(0);
        float[] result = new float[vector.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = vector.get(i);
        }
        return result;
    }

    record EmbedRequest(String model, String input,
    @JsonProperty("keep_alive") int keepAlive) {}

    record EmbedResponse(List<List<Float>> embeddings) {}
}