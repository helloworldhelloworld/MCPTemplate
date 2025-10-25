package com.example.mcp.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class HmacAuthFilterTest {

  private static final String SHARED_SECRET = "test-secret";

  private HmacAuthFilter filter;

  @BeforeEach
  void setUp() {
    filter = new HmacAuthFilter();
    ReflectionTestUtils.setField(filter, "sharedSecret", SHARED_SECRET);
  }

  @Test
  void doFilterAllowsValidSignature() throws Exception {
    MockHttpServletRequest request = buildRequestWithBody("{\"foo\":\"bar\"}");
    String timestamp = String.valueOf(Instant.now().toEpochMilli());
    request.addHeader("X-MCP-ClientId", "client-123");
    request.addHeader("X-MCP-Timestamp", timestamp);
    request.addHeader("X-MCP-Signature", sign(timestamp + "\n" + request.getContentAsString()));

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertEquals(1, chain.getRequestCount());
  }

  @Test
  void doFilterRejectsInvalidSignature() throws Exception {
    MockHttpServletRequest request = buildRequestWithBody("{\"foo\":\"bar\"}");
    String timestamp = String.valueOf(Instant.now().toEpochMilli());
    request.addHeader("X-MCP-ClientId", "client-123");
    request.addHeader("X-MCP-Timestamp", timestamp);
    request.addHeader("X-MCP-Signature", "invalid");

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(401, response.getStatus());
    assertEquals(0, chain.getRequestCount());

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
    assertEquals("error", node.path("status").asText());
    assertEquals("INVALID_SIGNATURE", node.path("code").asText());
  }

  private MockHttpServletRequest buildRequestWithBody(String body) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod("POST");
    request.setRequestURI("/mcp/invoke");
    request.setContentType("application/json");
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    return request;
  }

  private String sign(String material) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SHARED_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return Base64.getEncoder().encodeToString(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
  }
}
