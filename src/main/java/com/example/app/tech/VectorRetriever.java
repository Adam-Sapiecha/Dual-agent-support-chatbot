package com.example.app.tech;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class VectorRetriever {
private final List<DocChunk> chunks;
private final EmbeddingClient embedClient;


public VectorRetriever(List<DocChunk> chunks, EmbeddingClient embedClient) {
this.chunks = chunks;
this.embedClient = embedClient;
}


public List<DocChunk> topK(String query, int k) throws IOException, InterruptedException {
List<Double> qEmb = embedClient.getEmbedding(query);
return chunks.stream()
.sorted(Comparator.comparingDouble(c -> -cosineSimilarity(c.getEmbedding(), qEmb)))
.limit(k)
.toList();
}


private double cosineSimilarity(List<Double> a, List<Double> b) {
double dot = 0, normA = 0, normB = 0;
for (int i = 0; i < a.size(); i++) {
dot += a.get(i) * b.get(i);
normA += a.get(i) * a.get(i);
normB += b.get(i) * b.get(i);
}
return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}
}