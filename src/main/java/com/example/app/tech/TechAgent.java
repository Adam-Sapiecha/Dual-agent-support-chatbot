package com.example.app.tech;
import com.example.app.llm.ToolSpec;

import com.example.app.convo.Message;
import com.example.app.llm.LlmClient;

import java.util.*;
import java.util.stream.Collectors;

public class TechAgent {
  private final LlmClient llm;
  private final String model;
  private final VectorRetriever retriever;

  public TechAgent(LlmClient llm, String model, VectorRetriever retriever) {
    this.llm = llm;
    this.model = model;
    this.retriever = retriever;
  }

  public String answer(List<Message> history, String userText) {
    List<DocChunk> top;
    try {
      top = retriever.topK(userText, 5);
    } catch (Exception e) {
      return "[Błąd podczas przeszukiwania dokumentacji]";
    }

    String excerpts = top.isEmpty()
      ? "(no relevant documentation excerpts found)"
      : top.stream()
          .map(c -> "Źródło: " + c.getSource() + "\n" + c.getText())
          .collect(Collectors.joining("\n\n---\n\n"));

    List<Message> msgs = new ArrayList<>();
    msgs.add(Message.system("""
You are Agent A — Technical Specialist.
You must answer ONLY using the provided documentation excerpts.
If the excerpts do not contain the answer, say that the docs don't cover it and ask 1-2 clarifying questions. Do not guess.
When you use a fact, include a citation like [file#section].
"""));
    msgs.add(Message.system("Documentation excerpts:\n\n" + excerpts));

    int start = Math.max(0, history.size() - 8);
    for (int i = start; i < history.size(); i++) msgs.add(history.get(i));
    msgs.add(Message.user(userText));

    var res = llm.chat(model, msgs, List.<ToolSpec>of());

    return res.text == null ? "" : res.text.trim();
  }
}