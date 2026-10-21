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
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
  private final ObjectMapper om = new ObjectMapper();

  public BillingTools() {
    this(System.getenv("BILLING_API_BASE_URL"));
  }

  public BillingTools(String baseUrl) {
    this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? null : baseUrl.replaceAll("/+$", "");
  }

  public Map<String, Object> execute(String toolName, Map<String, Object> args) {
    if (toolName == null || args == null) return Map.of("error", "invalid_arguments");
    try {
      for (Object value : args.values()) {
        if (!(value instanceof String)) return Map.of("error", "invalid_arguments");
      }
      if ("open_refund_case".equals(toolName)) {
        Map<String, Object> validation = validateRefund(args);
        if (!validation.isEmpty()) return validation;
      }
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
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      return Map.of("error", "tool_execution_failed", "tool", toolName);
    }
  }

  private Map<String, Object> postJson(String path, Map<String, Object> body) throws Exception {
    String url = baseUrl + path;
    String json = om.writeValueAsString(body == null ? Map.of() : body);

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(java.time.Duration.ofSeconds(10))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      return Map.of(
          "error", "http_error",
          "status", resp.statusCode()
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
      try { if (purchaseDate != null) pd = LocalDate.parse(purchaseDate); }
      catch (Exception e) { return Map.of("error", "invalid_purchase_date"); }
      return refundTimeline(pd);
    }
    return Map.of("error", "unknown_tool", "tool", toolName);
  }


  public Map<String, Object> lookupPlanPrice(String plan) {
    plan = plan == null ? "" : plan.trim().toLowerCase(Locale.ROOT);
    if (plan.equals("basic")) return Map.of("plan","Basic","priceMonthly","$19", "mode", "sample");
    if (plan.equals("pro")) return Map.of("plan","Pro","priceMonthly","$49", "mode", "sample");
    if (plan.equals("enterprise")) return Map.of("plan","Enterprise","priceMonthly","custom", "mode", "sample");
    return Map.of("plan","unknown","priceMonthly","unknown");
  }

  public Map<String, Object> openRefundCase(String email, String reason) {
    if (!validEmail(email) || reason == null || reason.isBlank() || reason.length() > 2000)
      return Map.of("error", "email_and_reason_required");
    String caseId = "DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    return Map.of("caseId", caseId, "email", email, "status", "SIMULATED", "reason", reason, "mode", "sample");
  }

  public Map<String, Object> sendRefundForm(String email, String caseId) {
    if (!validEmail(email) || caseId == null || caseId.isBlank()) return Map.of("error", "email_and_case_required");
    return Map.of("email", email, "caseId", caseId, "form", "refund_request_form_v1", "status", "SIMULATED_NOT_SENT");
  }

  public Map<String, Object> refundTimeline(LocalDate purchaseDate) {
    if (purchaseDate != null && !new BillingPolicy().eligibleForRefund(purchaseDate, LocalDate.now()))
      return Map.of("eligible", false, "timeline", "Purchase is outside the 14-day refund window or in the future.");
    return Map.of("timeline", "If approved, refunds are processed within 5-10 business days.", "refundWindowDays", 14);
  }

  public static boolean validEmail(String email) {
    return email != null && email.length() <= 254 && email.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$");
  }

  private Map<String, Object> validateRefund(Map<String, Object> args) {
    String email = (String) args.get("email"), reason = (String) args.get("reason");
    if (!validEmail(email) || reason == null || reason.isBlank() || reason.length() > 2000)
      return Map.of("error", "email_and_reason_required");
    if (args.containsKey("purchaseDate")) {
      try {
        if (!new BillingPolicy().eligibleForRefund(LocalDate.parse((String) args.get("purchaseDate")), LocalDate.now()))
          return Map.of("error", "outside_refund_window");
      } catch (RuntimeException e) { return Map.of("error", "invalid_purchase_date"); }
    }
    return Map.of();
  }
}
