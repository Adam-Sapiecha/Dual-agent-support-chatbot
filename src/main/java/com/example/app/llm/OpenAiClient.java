package com.example.app.llm;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.example.app.convo.Message;
import com.fasterxml.jackson.databind.*;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.Scanner;

public class OpenAiClient implements LlmClient {

  private static final MediaType JSON =
      MediaType.get("application/json; charset=utf-8");

  private final OkHttpClient http = new OkHttpClient();
  private final ObjectMapper om = new ObjectMapper();
  private final String apiKey;

  public OpenAiClient(String apiKey) {

    // ✅ Jeśli API key nie został przekazany ani ustawiony w ENV → pytamy użytkownika
    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("⚠ OPENAI_API_KEY missing.");
      System.out.print("Please enter your OpenAI API key: ");

      Scanner scanner = new Scanner(System.in);
      apiKey = scanner.nextLine().trim();

      if (apiKey.isBlank()) {
        throw new IllegalArgumentException("API key cannot be empty.");
      }

      System.out.println("✅ API key accepted.\n");
    }

    this.apiKey = apiKey;
  }

  @Override
  public LlmResult chat(String model, List<Message> messages, List<ToolSpec> tools) {
    try {
      ObjectNode root = om.createObjectNode();
      root.put("model", model);

      ArrayNode msgs = root.putArray("messages");
      for (Message m : messages) {
        ObjectNode mn = msgs.addObject();
        mn.put("role", m.role);
        if (m.name != null && !m.name.isBlank()) mn.put("name", m.name);
        mn.put("content", m.content == null ? "" : m.content);
      }

      if (tools != null && !tools.isEmpty()) {
        ArrayNode toolsArr = root.putArray("tools");
        for (ToolSpec t : tools) {
          ObjectNode tool = toolsArr.addObject();
          tool.put("type", "function");

          ObjectNode fn = tool.putObject("function");
          fn.put("name", t.name);
          fn.put("description", t.description);
          fn.set("parameters", om.valueToTree(t.parameters));
        }
        root.put("tool_choice", "auto");
      }

      Request req = new Request.Builder()
          .url("https://api.openai.com/v1/chat/completions")
          .addHeader("Authorization", "Bearer " + apiKey)
          .post(RequestBody.create(om.writeValueAsString(root), JSON))
          .build();

      try (Response resp = http.newCall(req).execute()) {

        if (!resp.isSuccessful()) {
          throw new RuntimeException(
              "OpenAI HTTP " + resp.code() + ": " +
              (resp.body() == null ? "" : resp.body().string())
          );
        }

        String body = resp.body().string();
        JsonNode j = om.readTree(body);

        JsonNode msg = j.get("choices").get(0).get("message");
        LlmResult r = new LlmResult();

        JsonNode toolCalls = msg.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
          JsonNode tc = toolCalls.get(0);
          JsonNode fn = tc.get("function");

          LlmClient.ToolCall call = new LlmClient.ToolCall();
          call.name = fn.get("name").asText();

          String argsJson = fn.get("arguments").asText("{}");

          @SuppressWarnings("unchecked")
          Map<String, Object> args = om.readValue(argsJson, Map.class);

          call.arguments = args;
          r.toolCall = call;
          r.text = "";
          return r;
        }

        JsonNode content = msg.get("content");
        r.text = content == null ? "" : content.asText("");

        return r;
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
