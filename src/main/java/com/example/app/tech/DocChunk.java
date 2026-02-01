package com.example.app.tech;

public class DocChunk {
  public String source;
  public String section;
  public String text;

  public String cite() {
    return "[" + source + "#" + section + "]";
  }
}
