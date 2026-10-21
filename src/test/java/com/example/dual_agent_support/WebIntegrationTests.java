package com.example.dual_agent_support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebIntegrationTests {
  @LocalServerPort int port;
  HttpClient http = HttpClient.newHttpClient();
  HttpResponse<String> request(String path, String body) throws Exception {
    var req = HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
    if(body != null) req.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body));
    return http.send(req.build(),HttpResponse.BodyHandlers.ofString());
  }
  @Test void servesUiHealthAndSecurityHeaders() throws Exception {
    var r = request("/",null); assertEquals(200,r.statusCode()); assertTrue(r.body().contains("Support desk"));
    assertTrue(r.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'"));
    assertTrue(request("/api/health",null).body().contains("sample"));
  }
  @Test void chatContractAndBadInputs() throws Exception {
    var r = request("/api/chat","{\"message\":\"Pro plan pricing\"}");
    assertEquals(200,r.statusCode()); var json = new ObjectMapper().readTree(r.body());
    assertEquals("BILLING",json.get("route").asText()); assertTrue(json.get("answer").asText().contains("$49"));
    assertEquals(400,request("/api/chat","{}").statusCode());
    assertEquals(400,request("/api/chat","{").statusCode());
    assertEquals(413,request("/api/chat","x".repeat(12001)).statusCode());
    assertEquals(410,request("/api/chat","{\"sessionId\":\"unknown\",\"message\":\"hello\"}").statusCode());
  }
}
