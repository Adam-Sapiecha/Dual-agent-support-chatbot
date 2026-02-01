package com.example.app.routing;

import com.example.app.convo.Message;
import com.example.app.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class Router {
  private final LlmClient llm;
  private final ObjectMapper om = new ObjectMapper();
  private final String model;

  public Router(LlmClient llm, String model) {
    this.llm = llm;
    this.model = model;
  }

  public RouteDecision decide(List<Message> history, String userText) {
    List<Message> msgs = new ArrayList<>();
    msgs.add(Message.system("""
You are a router. Decide which specialist should answer the user's message.
Return ONLY strict JSON with keys: route, reason.
Allowed route values: TECH, BILLING, OUT_OF_SCOPE.
Rules:
- TECH: integration, API keys, authentication, webhooks, signatures, rate limits, HTTP error codes, troubleshooting.
- BILLING: plans, pricing, invoices, refunds, subscription, payment issues.
- OUT_OF_SCOPE: anything else.
"""));
    int start = Math.max(0, history.size() - 6);
    for (int i = start; i < history.size(); i++) msgs.add(history.get(i));
    msgs.add(Message.user(userText));

    var res = llm.chat(model, msgs, List.of());
    try {
      return om.readValue(res.text, RouteDecision.class);
    } catch (Exception e) {
      RouteDecision d = new RouteDecision();
      d.route = "OUT_OF_SCOPE";
      d.reason = "Router parse failed";
      return d;
    }
  }
}
