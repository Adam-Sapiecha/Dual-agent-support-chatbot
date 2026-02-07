package com.example.app.tech;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
public class DocIndexer {
private static final Path CACHE_PATH = Path.of("docsIndex.json");


public static List<DocChunk> build(String folder, EmbeddingClient embedClient)
throws IOException, InterruptedException {


Map<String, DocChunk> cache = new HashMap<>();
ObjectMapper om = new ObjectMapper();


if (Files.exists(CACHE_PATH)) {
List<DocChunk> cached = Arrays.asList(om.readValue(CACHE_PATH.toFile(), DocChunk[].class));
for (DocChunk c : cached) cache.put(c.getId(), c);
}


List<DocChunk> chunks = new ArrayList<>();
Path dir = Paths.get(folder);
for (Path file : Files.list(dir).toList()) {
String content = Files.readString(file);
String[] parts = content.split("\\n\\n");
for (int i = 0; i < parts.length; i++) {
if (parts[i].length() < 40) continue;
DocChunk c = new DocChunk(file.getFileName().toString(), i, parts[i]);
if (cache.containsKey(c.getId())) {
c.setEmbedding(cache.get(c.getId()).getEmbedding());
} else {
c.setEmbedding(embedClient.getEmbedding(c.getText()));
}
chunks.add(c);
}
}


om.writerWithDefaultPrettyPrinter().writeValue(CACHE_PATH.toFile(), chunks);
return chunks;
}
}