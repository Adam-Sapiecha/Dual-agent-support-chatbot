package com.example.app.billing;

import java.time.LocalDate;
import java.util.*;

public class BillingTools {
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
