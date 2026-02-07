package com.example.app.billing;

import com.example.app.convo.Message;
import com.example.app.llm.LlmClient;
import com.example.app.llm.ToolSpec;
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

  public String answer(List<Message> history, String userText) {
    List<Message> msgs = new ArrayList<>();

    msgs.add(Message.system("""
You are Agent B — Billing Specialist.
You can use tools to: look up plan pricing, open a refund case, send a refund form, and describe refund timelines.
Follow policy:
- Refund window: %d days from purchase date.
Rules:
- Use tools when you need structured data (pricing, case creation).
- If required fields are missing (email, invoice/case id, purchase date), ask the user.
- Do NOT invent invoice numbers, case ids, or prices.
- After opening a refund case: confirm caseId and offer the form.
""".formatted(policy.refundWindowDays)));

    int start = Math.max(0, history.size() - 8);
    for (int i = start; i < history.size(); i++) msgs.add(history.get(i));
    msgs.add(Message.user(userText));

    List<ToolSpec> toolSpecs = buildToolSpecs();


    int maxSteps = 6;

    for (int step = 0; step < maxSteps; step++) {
      var res = llm.chat(model, msgs, toolSpecs);


      if (res.toolCall != null && res.toolCall.name != null) {
        String toolName = res.toolCall.name;
        String toolCallId = res.toolCall.id;

        Map<String, Object> args = res.toolCall.arguments == null ? Map.of() : res.toolCall.arguments;
        Map<String, Object> toolResult = tools.execute(toolName, args);


        if ("open_refund_case".equals(toolName)) {
          Object caseId = toolResult.get("caseId");
          if (caseId != null) state.lastCaseId = String.valueOf(caseId);
          Object email = toolResult.get("email");
          if (email != null) state.customerEmail = String.valueOf(email);
        }


        String toolJson;
        try { toolJson = om.writeValueAsString(toolResult); }
        catch (Exception e) { toolJson = "{\"error\":\"serialize_failed\"}"; }

        msgs.add(Message.tool(toolName, toolCallId, toolJson));
        continue;
      }


      if (res.text != null && !res.text.isBlank()) {
        return res.text;
      }

      return "Please rephrase your billing request with details (email, invoice id, plan name).";
    }

    return "I couldn't complete the billing flow (too many tool steps). Please provide missing details (email, invoice id, purchase date).";
  }

  private List<ToolSpec> buildToolSpecs() {
    List<ToolSpec> specs = new ArrayList<>();

    specs.add(ToolSpec.of(
        "lookup_plan_price",
        "Lookup plan name and monthly price for a given plan identifier.",
        Map.of(
            "type", "object",
            "properties", Map.of(
                "plan", Map.of("type", "string", "description", "Plan name or identifier, e.g. Basic/Pro/Enterprise")
            ),
            "required", List.of("plan"),
            "additionalProperties", false
        )
    ));

    specs.add(ToolSpec.of(
        "open_refund_case",
        "Open a refund support case. Requires customer email and reason.",
        Map.of(
            "type", "object",
            "properties", Map.of(
                "email", Map.of("type", "string", "description", "Customer email"),
                "reason", Map.of("type", "string", "description", "Reason for refund request"),
                "purchaseDate", Map.of("type", "string", "description", "Optional purchase date YYYY-MM-DD"),
                "invoiceId", Map.of("type", "string", "description", "Optional invoice id")
            ),
            "required", List.of("email", "reason"),
            "additionalProperties", false
        )
    ));

    specs.add(ToolSpec.of(
        "send_refund_form",
        "Send a refund request form to a customer for a specific case.",
        Map.of(
            "type", "object",
            "properties", Map.of(
                "email", Map.of("type", "string"),
                "caseId", Map.of("type", "string")
            ),
            "required", List.of("email", "caseId"),
            "additionalProperties", false
        )
    ));

    specs.add(ToolSpec.of(
        "refund_timeline",
        "Return the typical refund processing timeline.",
        Map.of(
            "type", "object",
            "properties", Map.of(
                "purchaseDate", Map.of("type", "string", "description", "Optional purchase date YYYY-MM-DD")
            ),
            "required", List.of(),
            "additionalProperties", false
        )
    ));

    return specs;
  }
}
