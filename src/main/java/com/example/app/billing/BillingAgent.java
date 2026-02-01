package com.example.app.billing;

import com.example.app.convo.Message;
import com.example.app.llm.LlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.*;

public class BillingAgent {
  private final LlmClient llm;
  private final String model;
  private final BillingTools tools;
  private final BillingPolicy policy;
  private final BillingState state = new BillingState();
  private final ObjectMapper om = new ObjectMapper();

  public BillingAgent(LlmClient llm, String model, BillingTools tools, BillingPolicy policy) {
    this.llm = llm;
    this.model = model;
    this.tools = tools;
    this.policy = policy;
  }

  // Pseudo-tool calling: model outputs JSON {"action": "...", "args": {...}}
  private static boolean looksLikeJson(String s) {
    if (s == null) return false;
    String t = s.trim();
    return t.startsWith("{") && t.endsWith("}");
  }

  @SuppressWarnings("unchecked")
  private Map<String,Object> parseJsonObj(String s) {
    try {
      return om.readValue(s, Map.class);
    } catch (Exception e) {
      return Map.of("action", "ask_user", "args", Map.of("question", "Please rephrase your billing request."));
    }
  }

  public String answer(List<Message> historyNoTools, String userText) {
    List<Message> msgs = new ArrayList<>();
    msgs.add(Message.system("""
You are Agent B — Billing Specialist.

You can perform actions by replying with STRICT JSON ONLY:
{
  "action": "lookup_plan_price" | "open_refund_case" | "send_refund_form" | "refund_timeline" | "ask_user" | "none",
  "args": { ... }
}

Rules:
- If you need missing info (email, plan name, purchase date), use action "ask_user" with args: {"question":"..."}
- If you can answer directly without actions, use action "none" and include your answer in args: {"answer":"..."}
- Otherwise choose one action and provide required args:
  - lookup_plan_price: {"plan":"Pro"}
  - open_refund_case: {"email":"...", "reason":"..."}
  - send_refund_form: {"email":"...", "caseId":"..."}
  - refund_timeline: {"purchaseDate":"YYYY-MM-DD or empty"}

Refund policy:
- Refund window is 14 days from purchase date.
- If approved, refunds are processed within 5–10 business days.
"""));

    int start = Math.max(0, historyNoTools.size() - 8);
    for (int i = start; i < historyNoTools.size(); i++) msgs.add(historyNoTools.get(i));
    msgs.add(Message.user(userText));

    // Up to 4 action steps
    for (int step = 0; step < 4; step++) {
      var res = llm.chat(model, msgs, List.of());
      String txt = res.text == null ? "" : res.text.trim();

      if (!looksLikeJson(txt)) {
        // If model accidentally wrote plain text, accept it
        return txt;
      }

      Map<String,Object> obj = parseJsonObj(txt);
      String action = String.valueOf(obj.getOrDefault("action", "ask_user"));
      Object argsObj = obj.getOrDefault("args", Map.of());
      Map<String,Object> args = (argsObj instanceof Map) ? (Map<String,Object>) argsObj : Map.of();

      switch (action) {
        case "none" -> {
          return String.valueOf(args.getOrDefault("answer", "Okay."));
        }
        case "ask_user" -> {
          return String.valueOf(args.getOrDefault("question", "What billing help do you need?"));
        }
        case "lookup_plan_price" -> {
          Map<String,Object> toolResult = tools.lookupPlanPrice(String.valueOf(args.getOrDefault("plan","")));
          msgs.add(Message.assistant("ACTION_RESULT lookup_plan_price: " + toJson(toolResult)));
          msgs.add(Message.user("Now reply to the user using the ACTION_RESULT."));
        }
        case "open_refund_case" -> {
          String email = String.valueOf(args.getOrDefault("email",""));
          String reason = String.valueOf(args.getOrDefault("reason",""));
          Map<String,Object> toolResult = tools.openRefundCase(email, reason);
          state.customerEmail = email;
          state.lastCaseId = String.valueOf(toolResult.getOrDefault("caseId",""));
          msgs.add(Message.assistant("ACTION_RESULT open_refund_case: " + toJson(toolResult)));
          msgs.add(Message.user("Now reply to the user using the ACTION_RESULT. If needed, ask next question."));
        }
        case "send_refund_form" -> {
          String email = String.valueOf(args.getOrDefault("email",""));
          String caseId = String.valueOf(args.getOrDefault("caseId",""));
          Map<String,Object> toolResult = tools.sendRefundForm(email, caseId);
          msgs.add(Message.assistant("ACTION_RESULT send_refund_form: " + toJson(toolResult)));
          msgs.add(Message.user("Now reply to the user using the ACTION_RESULT."));
        }
        case "refund_timeline" -> {
          String pd = String.valueOf(args.getOrDefault("purchaseDate",""));
          LocalDate d = null;
          try { if (pd != null && !pd.isBlank()) d = LocalDate.parse(pd.trim()); } catch (Exception ignored) {}
          Map<String,Object> toolResult = tools.refundTimeline(d);
          msgs.add(Message.assistant("ACTION_RESULT refund_timeline: " + toJson(toolResult)));
          msgs.add(Message.user("Now reply to the user using the ACTION_RESULT."));
        }
        default -> {
          return "Billing: I can help with plans, pricing, invoices, subscriptions, and refunds. What do you need?";
        }
      }
    }

    return "Billing: action loop exceeded.";
  }

  private String toJson(Map<String,Object> m) {
    try { return om.writeValueAsString(m); }
    catch (Exception e) { return "{\"error\":\"json\"}"; }
  }
}
