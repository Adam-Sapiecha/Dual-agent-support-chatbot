package com.example.dual_agent_support;

import com.example.app.web.DemoChatService;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class DemoChatServiceTests {
  DemoChatService service;
  @BeforeEach void setup() throws Exception { service = new DemoChatService(); }
  @Test void technicalAnswerCitesRealSection() {
    var r = service.chat(null, "HTTP 401 API error");
    assertEquals("TECH", r.route());
    assertTrue(r.sources().stream().anyMatch(s -> s.text().contains("X-API-Key")));
    assertTrue(r.answer().contains(r.sources().get(0).id()));
    assertFalse(r.answer().contains("429"));
  }
  @Test void webhookRulesAreRetrieved() {
    var r = service.chat(null, "What is the webhook timeout and retry behavior?");
    assertEquals("TECH", r.route()); assertTrue(r.answer().contains("3 seconds")); assertTrue(r.answer().contains("5 times"));
  }
  @Test void billingFlowCollectsBothFieldsAndDoesNotDuplicate() {
    var r = service.chat(null, "I want a refund"); String id = r.sessionId();
    assertTrue(r.tools().isEmpty());
    r = service.chat(id, "recruiter@example.com"); assertTrue(r.tools().isEmpty());
    r = service.chat(id, "I was charged twice"); assertTrue(r.answer().contains("DEMO-"));
    assertEquals(java.util.List.of("open_refund_case"), r.tools());
    assertTrue(service.chat(id, "refund status").tools().isEmpty());
  }
  @Test void sessionsDoNotShareRefundDetails() {
    var first = service.chat(null, "I want a refund");
    service.chat(first.sessionId(), "isolated-test@example.org");
    var second = service.chat(null, "I want a refund because I was charged twice");
    assertFalse(second.answer().contains("isolated-test@example.org")); assertTrue(second.tools().isEmpty());
  }
  @Test void resetInvalidatesSession() {
    var r = service.chat(null, "Pro plan pricing"); service.reset(r.sessionId());
    var e = assertThrows(ResponseStatusException.class, () -> service.chat(r.sessionId(), "Pro plan pricing"));
    assertEquals(410, e.getStatusCode().value());
  }
  @Test void refundContextDoesNotCaptureTechnicalIssue() {
    var r = service.chat(null, "I want a refund");
    assertEquals("TECH", service.chat(r.sessionId(), "HTTP 401").route());
  }
  @Test void invalidDateCannotBeBypassedInNextTurn() {
    var r = service.chat(null, "I want a refund because charged twice on 2000-01-01");
    assertTrue(r.answer().contains("outside"));
    assertFalse(service.chat(r.sessionId(), "recruiter@example.com").answer().contains("created for"));
    assertTrue(service.chat(r.sessionId(), LocalDate.now().toString()).answer().contains("created for"));
  }
  @Test void missingKnowledgeAsksForClarification() {
    var r = service.chat(null, "API graphql federation");
    assertTrue(r.sources().isEmpty()); assertTrue(r.answer().contains("does not cover"));
  }
  @Test void unrelatedAndMixedQuestionsDoNotExecuteTools() {
    assertEquals("OUT_OF_SCOPE", service.chat(null, "Tell me a pancake recipe").route());
    assertEquals("CLARIFY", service.chat(null, "API error and Pro pricing").route());
  }
  @Test void enforcesInputAndTurnLimits() {
    assertThrows(ResponseStatusException.class, () -> service.chat(null, " "));
    assertThrows(ResponseStatusException.class, () -> service.chat(null, "x".repeat(2001)));
    var r = service.chat(null, "Pro price");
    for (int i = 1; i < 30; i++) service.chat(r.sessionId(), "Pro price");
    assertEquals(429, assertThrows(ResponseStatusException.class, () -> service.chat(r.sessionId(), "Pro price")).getStatusCode().value());
  }
  @Test void globalRateLimitIsBoundedAcrossSessions() {
    for (int i = 0; i < 120; i++) service.chat(null, "hello");
    assertEquals(429, assertThrows(ResponseStatusException.class, () -> service.chat(null, "hello")).getStatusCode().value());
  }
}
