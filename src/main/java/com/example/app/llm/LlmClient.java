package com.example.app.llm;

import com.example.app.convo.Message;

import java.util.List;
import java.util.Map;

public interface LlmClient {
  LlmResult chat(String model, List<Message> messages, List<ToolSpec> tools);

  class LlmResult {
    public String text;
    public ToolCall toolCall;
  }

  class ToolCall {
    public String id;
    public String name;
    public Map<String, Object> arguments;
  }
}
