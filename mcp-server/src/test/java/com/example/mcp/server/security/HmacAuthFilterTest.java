package com.example.mcp.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class HmacAuthFilterTest {

  private static final String SECRET = "test-secret";
  private final ObjectMapper mapper = new ObjectMapper();
  private HmacAuthFilter filter;

  @BeforeEach
  void setUp() {
    filter = new HmacAuthFilter();
    ReflectionTestUtils.setField(filter, "sharedSecret", SECRET);
  }

  @Test
  void shouldNotFilterSkipsNonInvokePaths() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/other");

    assertTrue(filter.shouldNotFilter(request));
  }

  @Test
  void missingHeadersRejected() throws Exception {
    MockHttpServletRequest request = baseRequest();
    request.removeHeader("X-MCP-Signature");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, new MockFilterChain());

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    JsonNode body = mapper.readTree(response.getContentAsString());
    assertEquals("MISSING_HEADERS", body.path("code").asText());
  }

  @Test
  void invalidTimestampRejected() throws Exception {
    MockHttpServletRequest request = baseRequest();
    request.addHeader("X-MCP-Timestamp", "not-a-number");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, new MockFilterChain());

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    JsonNode body = mapper.readTree(response.getContentAsString());
    assertEquals("INVALID_TIMESTAMP", body.path("code").asText());
  }

  @Test
  void signatureMismatchRejected() throws Exception {
    MockHttpServletRequest request = baseRequest();
    request.addHeader("X-MCP-Signature", "invalid");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, new MockFilterChain());

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    JsonNode body = mapper.readTree(response.getContentAsString());
    assertEquals("INVALID_SIGNATURE", body.path("code").asText());
  }

  @Test
  void validRequestPassesThrough() throws Exception {
    MockHttpServletRequest request = baseRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    TrackingFilterChain chain = new TrackingFilterChain();

    filter.doFilterInternal(request, response, chain);

    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    assertTrue(chain.invoked);
  }

  @Test
  void unexpectedErrorHandledGracefully() throws Exception {
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getRequestURI()).thenReturn("/mcp/invoke");
    Mockito.when(request.getHeader("X-MCP-ClientId")).thenReturn("client");
    Mockito.when(request.getHeader("X-MCP-Timestamp")).thenReturn("1");
    Mockito.when(request.getHeader("X-MCP-Signature")).thenReturn("sig");
    FilterChain chain = (servletRequest, servletResponse) -> {
      throw new ServletException("boom");
    };
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, chain);

    assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    JsonNode body = mapper.readTree(response.getContentAsString());
    assertEquals("HMAC_ERROR", body.path("code").asText());
    assertFalse(body.path("message").asText().isEmpty());
  }

  private MockHttpServletRequest baseRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/mcp/invoke");
    request.setMethod("POST");
    String body = "{\"hello\":\"world\"}";
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    request.addHeader("X-MCP-ClientId", "client");
    String timestamp = String.valueOf(Instant.now().toEpochMilli());
    request.addHeader("X-MCP-Timestamp", timestamp);
    request.addHeader("X-MCP-Signature", sign(timestamp + "\n" + body));
    return request;
  }

  private String sign(String material) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return Base64.getEncoder()
        .encodeToString(mac.doFinal(material.getBytes(StandardCharsets.UTF_8)));
  }

  private static class TrackingFilterChain extends MockFilterChain {
    private boolean invoked;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) {
      invoked = true;
    }
  }
}
