package com.example.app.tech;

import java.util.*;
import java.util.stream.Collectors;

public class Retriever {
  private final List<DocChunk> chunks;

  public Retriever(List<DocChunk> chunks) {
    this.chunks = chunks;
  }

  public List<DocChunk> topK(String query, int k) {
    Set<String> q = tok(query);

    return chunks.stream()
      .map(c -> Map.entry(c, score(q, tok(c.text + " " + c.section))))
      .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
      .filter(e -> e.getValue() > 0)
      .limit(k)
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  private int score(Set<String> q, Set<String> d) {
    int s = 0;
    for (String w : q) if (d.contains(w)) s++;
    return s;
  }

  private Set<String> tok(String s) {
    return Arrays.stream(s.toLowerCase().split("[^a-z0-9]+"))
      .filter(w -> w.length() >= 3)
      .collect(Collectors.toSet());
  }
}
