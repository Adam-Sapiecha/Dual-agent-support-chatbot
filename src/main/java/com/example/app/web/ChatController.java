package com.example.app.web;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ChatController {
  public record Request(String sessionId, String message) {}
  private final DemoChatService service;
  public ChatController(DemoChatService service) { this.service = service; }
  @GetMapping("/health") public Map<String, String> health() { return Map.of("status", "ok", "mode", "sample"); }
  @GetMapping("/docs") public List<DemoChatService.Source> docs() { return service.documents(); }
  @PostMapping(value = "/chat", consumes = "application/json")
  public DemoChatService.Reply chat(@RequestBody Request request) { return service.chat(request.sessionId(), request.message()); }
  @DeleteMapping(value = "/session", consumes = "application/json")
  public Map<String, Boolean> reset(@RequestBody Request request) { service.reset(request.sessionId()); return Map.of("reset", true); }
}
