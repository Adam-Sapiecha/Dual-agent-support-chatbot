package com.example.app.llm;

import com.example.app.convo.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class OpenAiClient implements LlmClient {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private static final String URL = "https://api.openai.com/v1/chat/completions";

  private final OkHttpClient http = new OkHttpClient();
  private final ObjectMapper om = new ObjectMapper();
  private final String apiKey;

  public OpenAiClient(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("OPENAI_API_KEY missing.");
      System.out.print("Please enter your OpenAI API key: ");
      Scanner scanner = new Scanner(System.in);
      apiKey = scanner.nextLine().trim();
      if (apiKey.isBlank()) throw new IllegalArgumentException("API key cannot be empty.");
      System.out.println("API key accepted.\n");
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

        if ("tool".equals(m.role) && m.toolCallId != null && !m.toolCallId.isBlank()) {
          mn.put("tool_call_id", m.toolCallId);
        }

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
          .url(URL)
          .addHeader("Authorization", "Bearer " + apiKey)
          .addHeader("Content-Type", "application/json")
          .post(RequestBody.create(om.writeValueAsString(root), JSON))
          .build();

      try (Response resp = http.newCall(req).execute()) {
        String body = resp.body() == null ? "" : resp.body().string();

        if (!resp.isSuccessful()) {
          throw new RuntimeException("OpenAI HTTP " + resp.code() + ": " + body);
        }

        JsonNode j = om.readTree(body);
        JsonNode choices = j.get("choices");
        if (choices == null || !choices.isArray() || choices.size() == 0) {
          throw new RuntimeException("OpenAI response missing choices: " + body);
        }

        JsonNode msg = choices.get(0).get("message");
        if (msg == null || msg.isNull()) {
          throw new RuntimeException("OpenAI response missing message: " + body);
        }

        LlmResult r = new LlmResult();

        JsonNode toolCalls = msg.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
          JsonNode tc = toolCalls.get(0);

          JsonNode fn = tc.get("function");
          if (fn == null || fn.isNull()) {
            throw new RuntimeException("Tool call missing function: " + body);
          }

          ToolCall call = new ToolCall();
          call.id = tc.has("id") ? tc.get("id").asText() : null;
          call.name = fn.has("name") ? fn.get("name").asText() : null;

          String argsJson = fn.has("arguments") ? fn.get("arguments").asText() : "{}";

          @SuppressWarnings("unchecked")
          Map<String, Object> args = om.readValue(argsJson, Map.class);

          call.arguments = args;
          r.toolCall = call;
          r.text = "";
          return r;
        }

        JsonNode content = msg.get("content");
        r.text = content == null || content.isNull() ? "" : content.asText("");
        r.toolCall = null;
        return r;
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
