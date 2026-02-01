package com.example.app.convo;

public class Message {
  public String role;
  public String content;
  public String name;

  public static Message user(String c) { var m = new Message(); m.role="user"; m.content=c; return m; }
  public static Message assistant(String c) { var m = new Message(); m.role="assistant"; m.content=c; return m; }
  public static Message system(String c) { var m = new Message(); m.role="system"; m.content=c; return m; }
  public static Message tool(String name, String c) { var m = new Message(); m.role="tool"; m.name=name; m.content=c; return m; }
}
