package com.example.mcp.server.security;

import com.example.mcp.common.StdResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;

/**
 * Reactive WebFilter that performs HMAC based request validation for MCP invocations.
 */
@Component
public class HmacSignatureWebFilter implements WebFilter {

  private static final Logger log = LoggerFactory.getLogger(HmacSignatureWebFilter.class);
  private static final Duration ALLOWED_DRIFT = Duration.ofMinutes(5);

  private final ObjectMapper objectMapper;
  private final String sharedSecret;

  public HmacSignatureWebFilter(
      ObjectMapper objectMapper, @Value("${mcp.security.secret:local-secret}") String sharedSecret) {
    this.objectMapper = objectMapper;
    this.sharedSecret = sharedSecret;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (!com.example.mcp.server.config.McpServerPaths.INVOKE_PATH.equals(path)) {
      return chain.filter(exchange);
    }

    ServerHttpRequest request = exchange.getRequest();
    String clientId = request.getHeaders().getFirst("X-MCP-ClientId");
    String timestampHeader = request.getHeaders().getFirst("X-MCP-Timestamp");
    String signature = request.getHeaders().getFirst("X-MCP-Signature");

    if (clientId == null || timestampHeader == null || signature == null) {
      log.warn("Missing HMAC headers for reactive request {}", request.getURI());
      return reject(exchange, HttpStatus.UNAUTHORIZED, "MISSING_HEADERS", "Missing HMAC authentication headers");
    }

    return DataBufferUtils.join(request.getBody())
        .publishOn(Schedulers.boundedElastic())
        .flatMap(buffer -> verifyAndContinue(buffer, exchange, chain, timestampHeader, signature, clientId))
        .onErrorResume(ResponseStatusException.class, ex -> reject(exchange, ex.getStatus(), "HMAC_ERROR", ex.getReason()))
        .onErrorResume(NumberFormatException.class, ex ->
            reject(exchange, HttpStatus.UNAUTHORIZED, "INVALID_TIMESTAMP", "Timestamp header must be epoch milliseconds"))
        .onErrorResume(Exception.class, ex -> {
          log.error("Unexpected error during HMAC validation", ex);
          return reject(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "HMAC_ERROR", "Failed to verify signature");
        });
  }

  private Mono<Void> verifyAndContinue(
      DataBuffer bodyBuffer,
      ServerWebExchange exchange,
      WebFilterChain chain,
      String timestampHeader,
      String signature,
      String clientId)
      throws Exception {
    byte[] bodyBytes = new byte[bodyBuffer.readableByteCount()];
    bodyBuffer.read(bodyBytes);
    DataBufferUtils.release(bodyBuffer);

    Instant requestInstant = Instant.ofEpochMilli(Long.parseLong(timestampHeader));
    Instant now = Instant.now();
    if (Duration.between(requestInstant, now).abs().compareTo(ALLOWED_DRIFT) > 0) {
      log.warn("Request timestamp drift exceeds tolerance");
      return reject(exchange, HttpStatus.UNAUTHORIZED, "TIMESTAMP_DRIFT", "Request timestamp outside tolerance");
    }

    String payload = new String(bodyBytes, StandardCharsets.UTF_8);
    String material = timestampHeader + "\n" + payload;
    String expected = computeSignature(material);
    if (!constantTimeEquals(expected, signature)) {
      log.warn("Invalid HMAC signature for client {}", clientId);
      return reject(exchange, HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "Signature verification failed");
    }

    ServerHttpRequest decorated = decorate(exchange.getRequest(), bodyBytes, exchange.getResponse().bufferFactory());
    return chain.filter(exchange.mutate().request(decorated).build());
  }

  private ServerHttpRequest decorate(ServerHttpRequest request, byte[] cachedBody, DataBufferFactory bufferFactory) {
    return new ServerHttpRequestDecorator(request) {
      @Override
      public Flux<DataBuffer> getBody() {
        DataBuffer buffer = bufferFactory.wrap(cachedBody);
        return Flux.just(buffer);
      }
    };
  }

  private String computeSignature(String material) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] raw = mac.doFinal(material.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(raw);
  }

  private boolean constantTimeEquals(@Nullable String expected, @Nullable String actual) {
    if (expected == null || actual == null || expected.length() != actual.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < expected.length(); i++) {
      result |= expected.charAt(i) ^ actual.charAt(i);
    }
    return result == 0;
  }

  private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    StdResponse<Void> error = StdResponse.error(code, message);
    return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(error))
        .flatMap(bytes -> exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))))
        .onErrorMap(ex -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize error", ex));
  }
}
