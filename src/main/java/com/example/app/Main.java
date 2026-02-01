package com.example.app;

import com.example.app.billing.*;
import com.example.app.convo.*;
import com.example.app.llm.*;
import com.example.app.routing.*;
import com.example.app.tech.*;
import com.example.app.routing.RouteDecision;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws Exception {
    String apiKey = System.getenv("OPENAI_API_KEY");
    var llm = new OpenAiClient(apiKey);
    String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");

    var docStore = new DocStore(Path.of("docs"));
    var retriever = new Retriever(docStore.allChunks());

    var tech = new TechAgent(llm, model, retriever);
    var billing = new BillingAgent(llm, model, new BillingTools(), new BillingPolicy());
    var router = new Router(llm, model);

    var convo = new ConversationStore();

    Scanner sc = new Scanner(System.in);
    while (true) {
      System.out.print("You: ");
      String user = sc.nextLine();
      if (user == null) break;
      user = user.trim();
      if (user.equalsIgnoreCase("exit") || user.equalsIgnoreCase("quit")) break;

      convo.add(Message.user(user));

      RouteDecision d = router.decide(convo.allForLlm(), user);

      String out;
      if ("TECH".equalsIgnoreCase(d.route)) {
        out = tech.answer(convo.allForLlm(), user);
      } else if ("BILLING".equalsIgnoreCase(d.route)) {
        out = billing.answer(convo.allForLlm(), user);
      } else {
        out = "I can help with technical integration/troubleshooting or billing (plans, pricing, invoices, refunds). What do you need?";
      }

      convo.add(Message.assistant(out));
      System.out.println("Assistant: " + out);

    }
  }
}
