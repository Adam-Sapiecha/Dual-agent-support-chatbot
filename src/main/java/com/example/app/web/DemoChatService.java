package com.example.app.web;

import com.example.app.billing.BillingTools;
import com.example.app.tech.DocChunk;
import com.example.app.tech.DocIndexer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

/** Public sample adapter: no model calls, remote billing, email, or durable personal data. */
@Service
public class DemoChatService {
  public record Source(String id, String text) {}
  public record Reply(String sessionId, String route, String reason, String answer,
                      List<Source> sources, List<String> tools, String mode, int turn) {}
  private static final Pattern TECH = Pattern.compile("(?iu)\\b(api|401|403|404|429|500|webhooks?|signature|timeout|hmac|authentication|token|integration|rate.?limit|błąd|blad|klucz|sygnatur\\w*)\\b");
  private static final Pattern BILLING = Pattern.compile("(?iu)\\b(billing|refund\\w*|price|pricing|plans?|pro|basic|enterprise|invoice|subscription|payment|zwrot\\w*|cena|ceny|płatno\\w*|faktur\\w*)\\b");
  private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+");
  private final List<DocChunk> docs;
  private final Map<String, Session> sessions = new HashMap<>();
  private final BillingTools billing = new BillingTools("");
  private long minute = -1;
  private int requests;
  private static class Session {
    Instant touched = Instant.now();
    int turns;
    boolean refund;
    String email, reason, caseId, lastTech, purchaseDate;
  }

  public DemoChatService() throws IOException {
    docs = DocIndexer.readSections(Path.of("docs"));
    if (docs.isEmpty()) throw new IOException("Support documentation is missing");
  }

  public List<Source> documents() {
    return docs.stream().map(c -> new Source(c.getId(), c.getText())).toList();
  }

  public synchronized Reply chat(String id, String message) {
    if (message == null || message.isBlank() || message.length() > 2000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must contain 1–2000 characters");
    long nowMinute = System.currentTimeMillis() / 60000;
    if (nowMinute != minute) { minute = nowMinute; requests = 0; }
    if (++requests > 120) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demo is busy; retry in a minute");
    expireSessions();
    Session s;
    if (id == null || id.isBlank()) {
      if (sessions.size() >= 300) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Demo session capacity reached");
      id = UUID.randomUUID().toString();
      s = new Session(); sessions.put(id, s);
    } else {
      s = sessions.get(id);
      if (s == null) throw new ResponseStatusException(HttpStatus.GONE, "Session expired; start a new conversation");
    }
    if (s.turns >= 30) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "30-turn limit; start a new conversation");
    s.touched = Instant.now(); s.turns++;
    String text = message.trim(), lower = text.toLowerCase(Locale.ROOT);
    boolean tech = TECH.matcher(text).find(), bill = BILLING.matcher(text).find();
    if (tech && bill) return reply(id, s, "CLARIFY", "Both specialist intents detected",
        "I see both a technical and a billing question. Please send one issue at a time, for example: ‘HTTP 401’ or ‘Pro plan pricing’.", List.of(), List.of());
    if (tech) { s.lastTech = text; return technical(id, s, text); }
    if (bill || (s.refund && !text.matches("(?iu).*(recipe|pancake|weather|joke|pogoda|przepis).*")))
      return billing(id, s, text, lower);
    if (s.lastTech != null && lower.matches("(?iu)(it still fails|still fails|why|how|dalej nie działa|co dalej|and now)[?.! ]*"))
      return technical(id, s, s.lastTech);
    return reply(id, s, "OUT_OF_SCOPE", "No supported intent matched",
        "I can help with API integration, authentication, webhooks, plan pricing and sample refunds. Try a scenario on the left. This rule-based demo supports a limited vocabulary; the CLI supports OpenAI routing.", List.of(), List.of());
  }

  private Reply technical(String id, Session s, String query) {
    Set<String> tokens = new HashSet<>(Arrays.asList(query.toLowerCase(Locale.ROOT).split("[^\\p{L}0-9_]+")));
    tokens.removeAll(Set.of("i", "a", "the", "is", "my", "what", "how", "do", "to", "with", "when", "and", "it", "get"));
    if (tokens.contains("401")) tokens.add("unauthorized");
    if (tokens.contains("429")) tokens.add("rate");
    if (tokens.stream().anyMatch(t -> t.contains("sygnatur") || t.equals("invalid_signature"))) tokens.add("signature");
    if (tokens.contains("klucz")) tokens.add("authentication");
    var ranked = docs.stream().map(c -> Map.entry(c, score(c, tokens)))
        .filter(e -> e.getValue() >= 2).sorted(Map.Entry.<DocChunk, Integer>comparingByValue().reversed())
        .limit(2).map(Map.Entry::getKey).toList();
    if (ranked.isEmpty()) return reply(id, s, "TECH", "Technical intent; no relevant section",
        "The sample documentation does not cover that question. Which endpoint, HTTP status or webhook error are you seeing? Please do not paste secrets.", List.of(), List.of("search_local_docs"));
    List<Source> sources = ranked.stream().map(c -> new Source(c.getId(), c.getText())).toList();
    String answer = "Here are the matching documentation excerpts (verbatim, not an AI-generated answer):\n\n" +
        sources.stream().map(c -> "[" + c.id() + "]\n" + c.text()).reduce((a,b) -> a + "\n\n" + b).orElse("");
    return reply(id, s, "TECH", "Technical keywords → local documentation search", answer, sources, List.of("search_local_docs"));
  }

  private int score(DocChunk c, Set<String> tokens) {
    String text = c.getText().toLowerCase(Locale.ROOT);
    String heading = text.lines().findFirst().orElse("");
    var status = Pattern.compile("\\b[45]\\d{2}\\b").matcher(text);
    Set<String> requestedStatuses = new HashSet<>();
    for (String token : tokens) if (token.matches("[45]\\d{2}")) requestedStatuses.add(token);
    if (status.find() && !requestedStatuses.isEmpty() && !requestedStatuses.contains(status.group())) return 0;
    int score = 0;
    for (String token : tokens) if (token.length() >= 3 && text.contains(token))
      score += token.matches("[45]\\d{2}") ? 12 : heading.contains(token) && !Set.of("api", "http").contains(token) ? 4 : 1;
    return score;
  }

  private Reply billing(String id, Session s, String text, String lower) {
    if (lower.matches(".*\\b(price|pricing|plan|plans|basic|pro|enterprise|cena|ceny)\\b.*")) {
      String plan = lower.matches(".*\\bbasic\\b.*") ? "Basic" : lower.matches(".*\\bpro\\b.*") ? "Pro" : lower.matches(".*\\benterprise\\b.*") ? "Enterprise" : null;
      if (plan == null) return reply(id, s, "BILLING", "Plan lookup needs a plan", "Which sample plan: Basic, Pro or Enterprise?", List.of(), List.of());
      var result = billing.execute("lookup_plan_price", Map.of("plan", plan));
      return reply(id, s, "BILLING", "Pricing intent → validated plan tool", "Sample catalog: " + plan + " costs " + result.get("priceMonthly") + " per month (Enterprise uses custom pricing). These are fictional demonstration prices.", List.of(), List.of("lookup_plan_price"));
    }
    if (lower.matches(".*(timeline|how long|ile trwa|termin).*"))
      return reply(id, s, "BILLING", "Refund timeline lookup", billing.refundTimeline(null).get("timeline") + " Sample policy: requests within 14 days of purchase.", List.of(), List.of("refund_timeline"));
    if (lower.matches(".*(refund|zwrot).*")) s.refund = true;
    if (!s.refund) return reply(id, s, "BILLING", "Billing topic outside sample tools", "This demo supports plan prices and sample refund cases. Invoice records and payment processing are not connected.", List.of(), List.of());
    if (s.caseId != null) return reply(id, s, "BILLING", "Existing sample case; no duplicate creation", "Your simulated case is " + s.caseId + ". No email was sent or money refunded. Start a new conversation to try again.", List.of(), List.of());
    var matcher = EMAIL.matcher(text);
    if (matcher.find()) s.email = matcher.group();
    // Never infer the reason from the initial generic refund request or an email-only turn.
    String reason = EMAIL.matcher(text).replaceAll("").replaceAll("\\b\\d{4}-\\d{2}-\\d{2}\\b", "")
        .replaceFirst("(?iu)^(my email is|email|e-mail)\\s*[:=]?\\s*", "").trim();
    if (reason.length() >= 5 && !reason.matches("(?iu)(i (want|need|would like) (a )?refund|refund|chcę zwrot)[.! ]*")) s.reason = reason;
    var date = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b").matcher(text);
    if (date.find()) s.purchaseDate = date.group();
    if (s.purchaseDate != null) {
      try {
        if (!new com.example.app.billing.BillingPolicy().eligibleForRefund(LocalDate.parse(s.purchaseDate), LocalDate.now()))
          return reply(id, s, "BILLING", "Refund policy rejected purchase date", "That date is outside the sample 14-day refund window or is in the future. No case was created. Supply an eligible date to continue.", List.of(), List.of("validate_refund_policy"));
      } catch (RuntimeException e) { return reply(id, s, "BILLING", "Invalid purchase date", "Please use a valid purchase date in YYYY-MM-DD format.", List.of(), List.of()); }
    }
    if (s.email == null || s.reason == null) return reply(id, s, "BILLING", "Refund flow waiting for required fields",
        "To simulate a refund case, provide " + (s.email == null ? "a sample email (e.g. recruiter@example.com)" : "") +
        (s.email == null && s.reason == null ? " and " : "") + (s.reason == null ? "a short reason" : "") + ". Use fictional data. Purchase date is optional; no eligibility is assumed without it.", List.of(), List.of());
    var result = billing.execute("open_refund_case", Map.of("email", s.email, "reason", s.reason));
    if (result.containsKey("error")) return reply(id, s, "BILLING", "Tool validation failed", "Please provide a valid sample email and reason.", List.of(), List.of("open_refund_case"));
    s.caseId = result.get("caseId").toString();
    return reply(id, s, "BILLING", "Required fields collected → sample case tool", "Simulated case " + s.caseId + " created for " + s.email + ". No email is sent, no refund is issued, and nothing is saved to a billing system. Typical sample processing time, if approved: 5–10 business days.", List.of(), List.of("open_refund_case"));
  }

  public synchronized void reset(String id) { if (id != null) sessions.remove(id); }
  @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000)
  public synchronized void expireSessions() {
    sessions.values().removeIf(s -> s.touched.isBefore(Instant.now().minus(Duration.ofMinutes(30))));
  }
  private Reply reply(String id, Session s, String route, String reason, String answer, List<Source> sources, List<String> tools) {
    return new Reply(id, route, reason, answer, sources, tools, "sample", s.turns);
  }
}
