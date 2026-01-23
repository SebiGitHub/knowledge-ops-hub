package dev.sebi.knowledgeopshub.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OllamaClient {

  private final RestClient client;
  private final String embedModel;
  private final String chatModel;

  public OllamaClient(
      @Value("${app.ollama.baseUrl}") String baseUrl,
      @Value("${app.ollama.embedModel}") String embedModel,
      @Value("${app.ollama.chatModel}") String chatModel
  ) {
    this.client = RestClient.builder().baseUrl(baseUrl).build();
    this.embedModel = embedModel;
    this.chatModel = chatModel;
  }

  // ---- Embeddings ----
  public record EmbeddingsRequest(String model, String prompt) {}
  public record EmbeddingsResponse(List<Double> embedding) {}

  public float[] embed(String text) {
    EmbeddingsResponse res = client.post()
        .uri("/api/embeddings")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new EmbeddingsRequest(embedModel, text))
        .retrieve()
        .body(EmbeddingsResponse.class);

    if (res == null || res.embedding() == null || res.embedding().isEmpty()) {
      throw new IllegalStateException("Ollama embedding vacío");
    }

    float[] v = new float[res.embedding().size()];
    for (int i = 0; i < v.length; i++) v[i] = res.embedding().get(i).floatValue();
    return v;
  }

  // ---- Generate (no streaming) ----
  public record GenerateRequest(String model, String prompt, boolean stream) {}
  public record GenerateResponse(String response, boolean done) {}

  public String generate(String prompt) {
    GenerateResponse res = client.post()
        .uri("/api/generate")
        .contentType(MediaType.APPLICATION_JSON)
        .body(new GenerateRequest(chatModel, prompt, false))
        .retrieve()
        .body(GenerateResponse.class);

    if (res == null || res.response() == null) {
      throw new IllegalStateException("Respuesta vacía de Ollama generate");
    }
    return res.response().trim();
  }
}
