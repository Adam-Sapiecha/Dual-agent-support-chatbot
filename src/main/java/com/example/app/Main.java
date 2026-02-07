package com.example.app;

import com.example.app.billing.BillingAgent;
import com.example.app.billing.BillingPolicy;
import com.example.app.billing.BillingTools;
import com.example.app.convo.ConversationStore;
import com.example.app.convo.Message;
import com.example.app.llm.LlmClient;
import com.example.app.llm.OpenAiClient;
import com.example.app.routing.RouteDecision;
import com.example.app.routing.Router;
import com.example.app.tech.DocChunk;
import com.example.app.tech.DocIndexer;
import com.example.app.tech.EmbeddingClient;
import com.example.app.tech.TechAgent;
import com.example.app.tech.VectorRetriever;

import java.util.List;
import java.util.Scanner;

public class Main {

  private static boolean looksLikeBillingFollowup(String user) {
    if (user == null) return false;
    String t = user.trim().toLowerCase();
    if (t.isEmpty()) return false;

    boolean hasEmail = t.matches(".*\\b[\\w.%-]+@[\\w.-]+\\.[a-z]{2,}\\b.*");
    boolean hasBillingKeywords = t.matches(".*\\b(refund|invoice|case|id|receipt|charge|payment)\\b.*");
    boolean isMostlyIdLike = t.matches(".*\\b\\d{1,8}\\b.*");

    return hasEmail || hasBillingKeywords || isMostlyIdLike;
  }

  public static void main(String[] args) throws Exception {
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("OPENAI_API_KEY missing. Set it in environment variables before running.");
      return;
    }

    String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o");

    LlmClient llm = new OpenAiClient(apiKey);

    var embeddingClient = new EmbeddingClient(apiKey);
    List<DocChunk> chunks = DocIndexer.build("docs", embeddingClient);
    var retriever = new VectorRetriever(chunks, embeddingClient);

    var tech = new TechAgent(llm, model, retriever);

    String billingBase = System.getenv().getOrDefault("BILLING_API_BASE_URL", "");
    var billing = new BillingAgent(llm, model, new BillingTools(billingBase), new BillingPolicy());

    var router = new Router(llm, model);
    var convo = new ConversationStore();

    String lastRoute = null;

    Scanner sc = new Scanner(System.in);
    while (true) {
      System.out.print("You: ");
      if (!sc.hasNextLine()) break;

      String user = sc.nextLine();
      if (user == null) break;

      user = user.trim();
      if (user.equalsIgnoreCase("exit") || user.equalsIgnoreCase("quit")) break;
      if (user.isBlank()) continue;

      convo.add(Message.user(user));

      RouteDecision d;
      if ("BILLING".equalsIgnoreCase(lastRoute) && looksLikeBillingFollowup(user)) {
        d = new RouteDecision();
        d.route = "BILLING";
        d.reason = "Sticky billing follow-up (email/id)";
      } else if ("TECH".equalsIgnoreCase(lastRoute) && !looksLikeBillingFollowup(user)) {
        // lekka “lepkość” dla TECH na krótkie doprecyzowania typu “ok”, “it still fails”, “401”
        boolean looksLikeTechFollowup =
            user.length() <= 40 ||
            user.toLowerCase().matches(".*\\b(401|403|404|500|timeout|error|fails|failing|stack|trace|api|token|key)\\b.*");

        if (looksLikeTechFollowup) {
          d = new RouteDecision();
          d.route = "TECH";
          d.reason = "Sticky tech follow-up";
        } else {
          d = router.decide(convo.allForLlm(), user);
        }
      } else {
        d = router.decide(convo.allForLlm(), user);
      }

      String out;
      if ("TECH".equalsIgnoreCase(d.route)) {
        out = tech.answer(convo.allForLlm(), user);
      } else if ("BILLING".equalsIgnoreCase(d.route)) {
        out = billing.answer(convo.allForLlm(), user);
      } else {
        out = "I can help with technical integration/troubleshooting or billing (plans, pricing, invoices, refunds). What do you need?";
      }

      lastRoute = d.route;

      convo.add(Message.assistant(out));
      System.out.println("Assistant: " + out);
    }
  }
}
