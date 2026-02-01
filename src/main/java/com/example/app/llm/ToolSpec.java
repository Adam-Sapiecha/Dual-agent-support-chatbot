package com.example.app.llm;

import java.util.*;

public class ToolSpec {
  public String name;
  public String description;
  public Map<String, Object> parameters;

  public static ToolSpec of(String name, String description, Map<String, Object> parameters) {
    ToolSpec t = new ToolSpec();
    t.name = name;
    t.description = description;
    t.parameters = parameters;
    return t;
  }
}
