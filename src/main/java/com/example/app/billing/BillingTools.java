package com.example.app.billing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

public class BillingTools {
  private final String baseUrl; 
  private final HttpClient http = HttpClient.newHttpClient();
  private final ObjectMapper om = new ObjectMapper();

  public BillingTools() {
    this(System.getenv("BILLING_API_BASE_URL"));
  }

  public BillingTools(String baseUrl) {
    this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? null : baseUrl.replaceAll("/+$", "");
  }

  public Map<String, Object> execute(String toolName, Map<String, Object> args) {
    try {
      if (baseUrl == null) {

        return executeLocal(toolName, args);
      }


      return switch (toolName) {
        case "lookup_plan_price" -> postJson("/lookup-plan-price", args);
        case "open_refund_case"  -> postJson("/open-refund-case", args);
        case "send_refund_form"  -> postJson("/send-refund-form", args);
        case "refund_timeline"   -> postJson("/refund-timeline", args);
        default -> Map.of("error", "unknown_tool", "tool", toolName);
      };

    } catch (Exception e) {
      return Map.of("error", "tool_execution_failed", "tool", toolName, "message", e.getMessage());
    }
  }

  private Map<String, Object> postJson(String path, Map<String, Object> body) throws Exception {
    String url = baseUrl + path;
    String json = om.writeValueAsString(body == null ? Map.of() : body);

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      return Map.of(
          "error", "http_error",
          "status", resp.statusCode(),
          "body", resp.body()
      );
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> parsed = om.readValue(resp.body(), Map.class);
    return parsed;
  }

  private Map<String, Object> executeLocal(String toolName, Map<String, Object> args) {
    if ("lookup_plan_price".equals(toolName)) {
      String plan = (String) args.getOrDefault("plan", "");
      return lookupPlanPrice(plan);
    }
    if ("open_refund_case".equals(toolName)) {
      String email = (String) args.getOrDefault("email", "");
      String reason = (String) args.getOrDefault("reason", "");
      return openRefundCase(email, reason);
    }
    if ("send_refund_form".equals(toolName)) {
      String email = (String) args.getOrDefault("email", "");
      String caseId = (String) args.getOrDefault("caseId", "");
      return sendRefundForm(email, caseId);
    }
    if ("refund_timeline".equals(toolName)) {
      String purchaseDate = (String) args.get("purchaseDate"); // optional
      LocalDate pd = null;
      try { if (purchaseDate != null) pd = LocalDate.parse(purchaseDate); } catch (Exception ignored) {}
      return refundTimeline(pd);
    }
    return Map.of("error", "unknown_tool", "tool", toolName);
  }


  public Map<String, Object> lookupPlanPrice(String plan) {
    plan = plan == null ? "" : plan.toLowerCase();
    if (plan.contains("basic")) return Map.of("plan","Basic","priceMonthly","$19");
    if (plan.contains("pro")) return Map.of("plan","Pro","priceMonthly","$49");
    if (plan.contains("enterprise")) return Map.of("plan","Enterprise","priceMonthly","custom");
    return Map.of("plan","unknown","priceMonthly","unknown");
  }

  public Map<String, Object> openRefundCase(String email, String reason) {
    String caseId = "CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    return Map.of("caseId", caseId, "email", email, "status", "OPENED", "reason", reason == null ? "" : reason);
  }

  public Map<String, Object> sendRefundForm(String email, String caseId) {
    return Map.of("sentTo", email, "caseId", caseId, "form", "refund_request_form_v1");
  }

  public Map<String, Object> refundTimeline(LocalDate purchaseDate) {
    return Map.of("timeline", "If approved, refunds are processed within 5-10 business days.");
  }
}
