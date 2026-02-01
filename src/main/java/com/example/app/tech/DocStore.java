package com.example.app.tech;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DocStore {
  private final List<DocChunk> chunks = new ArrayList<>();

  public DocStore(Path docsDir) throws IOException {
    try (var stream = Files.list(docsDir)) {
      stream.filter(p -> p.toString().endsWith(".md")).forEach(p -> {
        try { loadFile(p); } catch (IOException e) { throw new RuntimeException(e); }
      });
    }
  }

  private void loadFile(Path file) throws IOException {
    String content = Files.readString(file);
    String filename = file.getFileName().toString();

    String[] parts = content.split("\n## ");
    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      String section = (i == 0) ? "intro" : part.split("\n", 2)[0].trim();
      String body = (i == 0) ? part.trim() : part.substring(section.length()).trim();
      if (body.isBlank()) continue;

      DocChunk c = new DocChunk();
      c.source = filename;
      c.section = section.replace(" ", "-").toLowerCase();
      c.text = body.strip();
      chunks.add(c);
    }
  }

  public List<DocChunk> allChunks() {
    return chunks;
  }
}
