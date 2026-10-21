package com.example.app.tech;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DocIndexer {
  private static final Path CACHE_PATH = Path.of("docsIndex.json");

  public static List<DocChunk> build(String folder, EmbeddingClient embedClient)
      throws IOException, InterruptedException {
    return build(folder, embedClient, CACHE_PATH);
  }

  public static List<DocChunk> build(String folder, EmbeddingClient embedClient, Path cachePath)
      throws IOException, InterruptedException {
    Map<String, DocChunk> cache = new HashMap<>();
    ObjectMapper om = new ObjectMapper();
    if (Files.exists(cachePath)) {
      try {
        for (DocChunk c : om.readValue(cachePath.toFile(), DocChunk[].class)) cache.put(c.getId(), c);
      } catch (IOException ignored) { /* A damaged cache is safe to rebuild. */ }
    }
    List<DocChunk> chunks = readSections(Path.of(folder));
    for (DocChunk c : chunks) {
      DocChunk previous = cache.get(c.getId());
      c.setEmbedding(previous != null && Objects.equals(previous.getText(), c.getText())
          && previous.getEmbedding() != null ? previous.getEmbedding() : embedClient.getEmbedding(c.getText()));
    }
    Path temporary = Files.createTempFile(cachePath.toAbsolutePath().getParent(), "docsIndex-", ".tmp");
    try {
      om.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), chunks);
      Files.move(temporary, cachePath, StandardCopyOption.REPLACE_EXISTING);
    } finally { Files.deleteIfExists(temporary); }
    return chunks;
  }

  public static List<DocChunk> readSections(Path folder) throws IOException {
    List<DocChunk> chunks = new ArrayList<>();
    try (var files = Files.list(folder)) {
      for (Path file : files.filter(Files::isRegularFile)
          .filter(p -> Set.of("api_errors.md", "integrations.md", "security_faq.md", "troubleshooting.md")
              .contains(p.getFileName().toString())).sorted().toList()) {
        String[] sections = Files.readString(file).split("(?m)(?=^## )");
        for (int i = 0; i < sections.length; i++) {
          String text = sections[i].trim();
          if (!text.isEmpty()) chunks.add(new DocChunk(file.getFileName().toString(), i, text));
        }
      }
    }
    return List.copyOf(chunks);
  }
}
