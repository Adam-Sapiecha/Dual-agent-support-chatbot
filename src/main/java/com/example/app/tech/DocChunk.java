package com.example.app.tech;

import java.util.List;

public class DocChunk {
  private String id;
  private String source;
  private String text;
  private List<Double> embedding;


  public DocChunk() {}

  public DocChunk(String source, int sectionIndex, String text) {
    this.source = source;
    this.id = source + "#" + sectionIndex;
    this.text = text;
    this.embedding = null;
  }

  public DocChunk(String id, String source, String text, List<Double> embedding) {
    this.id = id;
    this.source = source;
    this.text = text;
    this.embedding = embedding;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }

  public String getText() { return text; }
  public void setText(String text) { this.text = text; }

  public List<Double> getEmbedding() { return embedding; }
  public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
}
