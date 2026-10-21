package com.example.app.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class SecurityHeaders implements Filter {
  @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var req = (HttpServletRequest) request;
    var res = (HttpServletResponse) response;
    res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Referrer-Policy", "no-referrer");
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
    if (req.getRequestURI().startsWith("/api")) res.setHeader("Cache-Control", "no-store");
    if (req.getContentLengthLong() > 12000) { res.sendError(413); return; }
    // Also cap chunked requests, which have no Content-Length.
    if (SetLike.isWrite(req.getMethod())) {
      byte[] bytes = req.getInputStream().readNBytes(12001);
      if (bytes.length > 12000) { res.sendError(413); return; }
      final var input = new java.io.ByteArrayInputStream(bytes);
      var wrapper = new HttpServletRequestWrapper(req) {
        @Override public ServletInputStream getInputStream() {
          return new ServletInputStream() {
            public int read() { return input.read(); }
            public boolean isFinished() { return input.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
          };
        }
      };
      chain.doFilter(wrapper, response); return;
    }
    chain.doFilter(request, response);
  }
  private static class SetLike {
    static boolean isWrite(String method) { return method.equals("POST") || method.equals("DELETE") || method.equals("PUT"); }
  }
}
