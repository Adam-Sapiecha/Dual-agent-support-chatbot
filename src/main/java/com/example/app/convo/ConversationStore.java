package com.example.app.convo;

import java.util.*;
import java.util.stream.Collectors;

public class ConversationStore {
  private final List<Message> messages = new ArrayList<>();

  public void add(Message m) { messages.add(m); }

  public List<Message> allRaw() { return messages; }

  public List<Message> allForLlm() {
    return messages.stream()
      .filter(m -> !"tool".equalsIgnoreCase(m.role))
      .collect(Collectors.toList());
  }
}
