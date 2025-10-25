package com.example.mcp.server.security;

import com.example.mcp.common.StdResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HmacAuthFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(HmacAuthFilter.class);
  private static final Duration ALLOWED_DRIFT = Duration.ofMinutes(5);

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${mcp.security.secret:local-secret}")
  private String sharedSecret;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"/mcp/invoke".equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
    String clientId = cachedRequest.getHeader("X-MCP-ClientId");
    String timestampHeader = cachedRequest.getHeader("X-MCP-Timestamp");
    String signature = cachedRequest.getHeader("X-MCP-Signature");

    if (clientId == null || timestampHeader == null || signature == null) {
      log.warn("Missing HMAC headers for request {}", request.getRequestURI());
      reject(response, HttpStatus.UNAUTHORIZED, "MISSING_HEADERS", "Missing HMAC authentication headers");
      return;
    }

    try {
      Instant requestInstant = Instant.ofEpochMilli(Long.parseLong(timestampHeader));
      Instant now = Instant.now();
      if (Duration.between(requestInstant, now).abs().compareTo(ALLOWED_DRIFT) > 0) {
        log.warn("Request timestamp drift exceeds tolerance for client {}", clientId);
        reject(response, HttpStatus.UNAUTHORIZED, "TIMESTAMP_DRIFT", "Request timestamp outside tolerance");
        return;
      }

      String payload = cachedRequest.getCachedBodyAsString();
      String material = timestampHeader + "\n" + payload;
      String expected = computeSignature(material);
      if (!constantTimeEquals(expected, signature)) {
        log.warn("Invalid HMAC signature for client {}", clientId);
        reject(response, HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "Signature verification failed");
        return;
      }

      filterChain.doFilter(cachedRequest, response);
    } catch (NumberFormatException ex) {
      log.warn("Invalid timestamp header", ex);
      reject(response, HttpStatus.UNAUTHORIZED, "INVALID_TIMESTAMP", "Timestamp header must be epoch milliseconds");
    } catch (Exception ex) {
      log.error("Unexpected error in HMAC filter", ex);
      reject(response, HttpStatus.INTERNAL_SERVER_ERROR, "HMAC_ERROR", "Failed to verify signature");
    }
  }

  private String computeSignature(String material) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] raw = mac.doFinal(material.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(raw);
  }

  private boolean constantTimeEquals(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    if (expected.length() != actual.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < expected.length(); i++) {
      result |= expected.charAt(i) ^ actual.charAt(i);
    }
    return result == 0;
  }

  private void reject(
      HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    StdResponse<Void> error = StdResponse.error(code, message);
    response.getWriter().write(objectMapper.writeValueAsString(error));
  }
}
