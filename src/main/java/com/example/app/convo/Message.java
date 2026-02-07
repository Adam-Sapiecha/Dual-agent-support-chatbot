package com.example.app.convo;

public class Message {
  public String role;
  public String content;
  public String name;

  // REQUIRED for tool outputs
  public String toolCallId;

  public static Message user(String c) {
    Message m = new Message();
    m.role = "user";
    m.content = c;
    return m;
  }

  public static Message assistant(String c) {
    Message m = new Message();
    m.role = "assistant";
    m.content = c;
    return m;
  }

  public static Message system(String c) {
    Message m = new Message();
    m.role = "system";
    m.content = c;
    return m;
  }

  public static Message tool(String name, String toolCallId, String content) {
    Message m = new Message();
    m.role = "tool";
    m.name = name;
    m.toolCallId = toolCallId;
    m.content = content;
    return m;
  }
}
