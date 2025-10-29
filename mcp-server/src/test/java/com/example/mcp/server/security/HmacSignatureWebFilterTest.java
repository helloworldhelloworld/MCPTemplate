package com.example.mcp.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class HmacSignatureWebFilterTest {

  private static final String SECRET = "test-secret";
  private final ObjectMapper mapper = new ObjectMapper();
  private HmacSignatureWebFilter filter;

  @BeforeEach
  void setUp() {
    filter = new HmacSignatureWebFilter(mapper, SECRET);
  }

  @Test
  void missingHeadersAreRejected() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.post("/mcp/invoke").body("{}"));

    filter.filter(exchange, new NoopChain()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    assertEquals("MISSING_HEADERS", readResponse(exchange).path("code").asText());
  }

  @Test
  void invalidSignatureIsRejected() {
    MockServerHttpRequest request =
        signedRequest("{\"foo\":1}")
            .header("X-MCP-Signature", "bad-signature")
            .build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    filter.filter(exchange, new NoopChain()).block();

    assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    assertEquals("INVALID_SIGNATURE", readResponse(exchange).path("code").asText());
  }

  @Test
  void validRequestDelegatesToChain() {
    MockServerHttpRequest request = signedRequest("{\"hello\":\"world\"}").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    TrackingChain chain = new TrackingChain();

    filter.filter(exchange, chain).block();

    assertTrue(chain.invoked);
    assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
  }

  private MockServerHttpRequest.Builder signedRequest(String body) {
    String timestamp = String.valueOf(Instant.now().toEpochMilli());
    String signature = sign(timestamp + "\n" + body);
    return MockServerHttpRequest.post("/mcp/invoke")
        .header("X-MCP-ClientId", "client")
        .header("X-MCP-Timestamp", timestamp)
        .header("X-MCP-Signature", signature)
        .body(body);
  }

  private JsonNode readResponse(MockServerWebExchange exchange) {
    return Flux.from(exchange.getResponse().getBody())
        .map(this::toBytes)
        .reduce(this::concat)
        .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
        .map(
            json -> {
              try {
                return mapper.readTree(json);
              } catch (Exception ex) {
                throw new RuntimeException(ex);
              }
            })
        .block();
  }

  private byte[] toBytes(DataBuffer buffer) {
    byte[] bytes = new byte[buffer.readableByteCount()];
    buffer.read(bytes);
    DataBufferUtils.release(buffer);
    return bytes;
  }

  private byte[] concat(byte[] first, byte[] second) {
    byte[] result = new byte[first.length + second.length];
    System.arraycopy(first, 0, result, 0, first.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  private String sign(String material) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return Base64.getEncoder().encodeToString(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static class NoopChain implements WebFilterChain {
    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange) {
      return Mono.empty();
    }
  }

  private static class TrackingChain implements WebFilterChain {
    private boolean invoked;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange) {
      invoked = true;
      exchange.getResponse().setStatusCode(HttpStatus.OK);
      return exchange.getResponse().setComplete();
    }
  }
}
