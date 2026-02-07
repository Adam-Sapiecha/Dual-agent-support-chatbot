package com.example.app.tech;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class EmbeddingClient {
  private final String apiKey;
  private final HttpClient http;
  private final ObjectMapper om = new ObjectMapper();

  public EmbeddingClient(String apiKey) {
    this.apiKey = apiKey;
    this.http = HttpClient.newHttpClient();
  }

  public List<Double> getEmbedding(String text) throws IOException, InterruptedException {
    String json = """
    {
      "model": "text-embedding-3-small",
      "input": %s
    }
    """.formatted(om.writeValueAsString(text));

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("https://api.openai.com/v1/embeddings"))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

    
    if (res.statusCode() < 200 || res.statusCode() >= 300) {
      throw new RuntimeException("Embeddings HTTP " + res.statusCode() + ": " + res.body());
    }

    JsonNode root = om.readTree(res.body());

    JsonNode data = root.get("data");
    if (data == null || !data.isArray() || data.size() == 0) {
      throw new RuntimeException("Embeddings response missing data: " + res.body());
    }

    JsonNode emb = data.get(0).get("embedding");
    if (emb == null || !emb.isArray()) {
      throw new RuntimeException("Embeddings response missing embedding: " + res.body());
    }

    List<Double> result = new ArrayList<>(emb.size());
    for (JsonNode num : emb) result.add(num.asDouble());
    return result;
  }
}
