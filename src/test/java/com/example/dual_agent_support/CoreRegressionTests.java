package com.example.dual_agent_support;

import com.example.app.billing.*;
import com.example.app.convo.Message;
import com.example.app.llm.*;
import com.example.app.routing.Router;
import com.example.app.tech.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreRegressionTests {
  @TempDir Path temp;
  @Test void toolResultHasPrecedingAssistantCall() {
    LlmClient llm = new LlmClient() {
      int calls;
      public LlmResult chat(String model, List<Message> messages, List<ToolSpec> tools) {
        var r = new LlmResult();
        if (calls++ == 0) {
          var tc = new ToolCall(); tc.id = "call-1"; tc.name = "lookup_plan_price"; tc.arguments = Map.of("plan", "Pro"); r.toolCall = tc;
        } else {
          var output = messages.get(messages.size()-1); var invocation = messages.get(messages.size()-2);
          assertEquals("assistant", invocation.role); assertNotNull(invocation.toolCall);
          assertEquals(invocation.toolCall.id, output.toolCallId); assertEquals("tool", output.role);
          assertTrue(output.content.contains("$49")); r.text = "Sample Pro costs $49";
        }
        return r;
      }
    };
    assertTrue(new BillingAgent(llm, "test", new BillingTools(""), new BillingPolicy()).answer(List.of(), "Pro price").contains("$49"));
  }
  @Test void invalidRouterOutputFailsClosed() {
    for (String text : List.of("null", "{}", "{\"route\":\"ADMIN\"}", "not json")) {
      LlmClient llm = (m,h,t) -> { var r = new LlmClient.LlmResult(); r.text = text; return r; };
      assertEquals("OUT_OF_SCOPE", new Router(llm, "test").decide(List.of(), "hello").route);
    }
  }
  @Test void refundPolicyBoundaryAndFuture() {
    var p = new BillingPolicy(); var now = LocalDate.of(2026,10,21);
    assertTrue(p.eligibleForRefund(now.minusDays(14),now)); assertFalse(p.eligibleForRefund(now.minusDays(15),now));
    assertFalse(p.eligibleForRefund(now.plusDays(1),now)); assertFalse(p.eligibleForRefund(null,now));
  }
  @Test void toolsValidateAndClearlySimulate() {
    var t = new BillingTools("");
    assertTrue(t.execute("open_refund_case", Map.of("email", "bad", "reason", "twice")).containsKey("error"));
    assertTrue(t.execute("open_refund_case", Map.of("email", "a@example.com", "reason", "twice", "purchaseDate", "2000-01-01")).containsKey("error"));
    assertTrue(t.execute("lookup_plan_price", Map.of("plan", 12)).containsKey("error"));
    assertEquals("unknown", t.lookupPlanPrice("unproven").get("plan"));
    assertEquals("SIMULATED_NOT_SENT", t.sendRefundForm("a@example.com", "DEMO-1").get("status"));
  }
  @Test void embeddingCacheInvalidatesChangedTextAndSkipsAuditDocs() throws Exception {
    Files.writeString(temp.resolve("integrations.md"), "## Authentication\nUse X-API-Key.");
    Files.writeString(temp.resolve("project-audit.md"), "private operational details");
    int[] calls = {0};
    var client = new EmbeddingClient("test") {
      @Override public List<Double> getEmbedding(String text) { calls[0]++; return List.of(1d,2d); }
    };
    Path cache = temp.resolve("cache.json");
    assertEquals(1, DocIndexer.build(temp.toString(),client,cache).size());
    DocIndexer.build(temp.toString(),client,cache); assertEquals(1,calls[0]);
    Files.writeString(temp.resolve("integrations.md"), "## Authentication\nUpdated key rules.");
    DocIndexer.build(temp.toString(),client,cache); assertEquals(2,calls[0]);
    Files.writeString(cache,"broken json"); DocIndexer.build(temp.toString(),client,cache); assertEquals(3,calls[0]);
  }
}
