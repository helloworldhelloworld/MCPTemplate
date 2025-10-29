package com.example.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

import com.example.mcp.client.transport.Transport;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class McpClientTest {

  @Test
  void openSessionCachesServerProtocol() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());
    Mockito
        .when(transport.postJson(eq("/mcp/session"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"SESSION_OPENED\",\"data\":{"
                + "\"sessionId\":\"s-1\",\"serverName\":\"demo\",\"protocol\":{"
                + "\"invoke\":\"/api/mcp/invoke\",\"discovery\":\"/api/mcp/tools\","
                + "\"stream\":\"/api/mcp/stream\",\"governance\":\"/api/mcp/governance\"},"
                + "\"tools\":[{\"name\":\"mcp.translation.invoke\",\"title\":\"翻译工具\"}]}}"
                );

    StdResponse<com.example.mcp.common.protocol.SessionOpenResponse> response = client.openSession(null);

    assertEquals("SESSION_OPENED", response.getCode());
    assertEquals("s-1", response.getData().getSessionId());
    assertEquals(1, client.getCachedTools().size());
    assertTrue(client.findTool("mcp.translation.invoke").isPresent());
  }

  @Test
  void discoverToolsParsesDescriptors() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());
    Mockito
        .when(transport.getJson("/mcp/tools"))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"TOOLS\",\"data\":[{\"name\":\"mcp.translation.invoke\",\"title\":\"翻译工具\"}]}"
                );

    StdResponse<java.util.List<ToolDescriptor>> response = client.discoverTools();

    assertEquals(1, response.getData().size());
    assertEquals("mcp.translation.invoke", response.getData().get(0).getName());
    assertTrue(client.findTool("mcp.translation.invoke").isPresent());
  }

  @Test
  void invokeUsesNegotiatedPath() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());
    Mockito
        .when(transport.postJson(eq("/mcp/session"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"SESSION_OPENED\",\"data\":{"
                + "\"sessionId\":\"s-1\",\"protocol\":{\"invoke\":\"/api/mcp/invoke\"},"
                + "\"tools\":[{\"name\":\"tool.name\"}]}}"
                );
    Mockito
        .when(transport.postJson(eq("/api/mcp/invoke"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"OK\",\"data\":{\"value\":99}}"
                );

    client.openSession(null);

    try (MockedStatic<Span> spanMock = Mockito.mockStatic(Span.class)) {
      spanMock.when(Span::current).thenReturn(null);
      StdResponse<ResponsePayload> response =
          client.invoke("tool.name", new RequestPayload("hello"), ResponsePayload.class);
      assertEquals(99, response.getData().value);
    }

    verify(transport).postJson(eq("/api/mcp/invoke"), any());
  }

  @Test
  void fetchGovernanceReportDelegatesToTransport() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());
    Mockito
        .when(transport.getJson("/mcp/governance/audit/req-1"))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"AUDIT\",\"data\":{\"events\":[{\"requestId\":\"req-1\"}]}}"
                );

    StdResponse<GovernanceReport> response = client.fetchGovernanceReport("req-1");

    assertEquals(1, response.getData().getEvents().size());
    assertEquals("req-1", response.getData().getEvents().get(0).getRequestId());
  }

  @Test
  void invokeParsesStdResponse() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());
    AtomicReference<String> payloadHolder = new AtomicReference<>();

    Mockito
        .when(transport.postJson(eq("/mcp/session"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"SESSION_OPENED\",\"data\":{"
                + "\"sessionId\":\"s-1\",\"tools\":[{\"name\":\"tool.name\"}]}}"
                );
    client.openSession(null);

    try (MockedStatic<Span> spanMock = Mockito.mockStatic(Span.class)) {
      Span span = Mockito.mock(Span.class);
      SpanContext spanContext = Mockito.mock(SpanContext.class);
      spanMock.when(Span::current).thenReturn(span);
      Mockito.when(span.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(span);
      Mockito.when(span.getSpanContext()).thenReturn(spanContext);
      Mockito.when(spanContext.isValid()).thenReturn(true);
      Mockito.when(spanContext.getTraceId()).thenReturn("trace-id");
      Mockito.when(transport.postJson(eq("/mcp/invoke"), any()))
          .thenAnswer(
              invocation -> {
                payloadHolder.set(invocation.getArgument(1));
                return "{\"status\":\"success\",\"code\":\"OK\",\"message\":\"done\",\"data\":{\"value\":42}}";
              });

      StdResponse<ResponsePayload> result =
          client.invoke("tool.name", new RequestPayload("hello"), ResponsePayload.class);

      assertEquals("OK", result.getCode());
      assertEquals(42, result.getData().value);
      assertNotNull(payloadHolder.get());
      spanMock.verify(Span::current, Mockito.atLeastOnce());
    }
  }

  @Test
  void invokeParsesResponseEnvelope() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());

    Mockito
        .when(transport.postJson(eq("/mcp/session"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"SESSION_OPENED\",\"data\":{"
                + "\"sessionId\":\"s-1\",\"tools\":[{\"name\":\"tool.name\"}]}}"
                );
    client.openSession(null);

    try (MockedStatic<Span> spanMock = Mockito.mockStatic(Span.class)) {
      Span span = Mockito.mock(Span.class);
      SpanContext spanContext = Mockito.mock(SpanContext.class);
      spanMock.when(Span::current).thenReturn(span);
      Mockito.when(span.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(span);
      Mockito.when(span.getSpanContext()).thenReturn(spanContext);
      Mockito.when(spanContext.isValid()).thenReturn(true);
      Mockito.when(spanContext.getTraceId()).thenReturn("trace-id");
      Mockito.when(transport.postJson(eq("/mcp/invoke"), any()))
          .thenReturn(
              "{\"tool\":\"tool.name\",\"context\":{\"clientId\":\"client-1\",\"traceId\":\"trace-id\"},"
                  + "\"response\":{\"status\":\"success\",\"code\":\"OK\",\"message\":\"done\",\"data\":{\"value\":7}}}");

      StdResponse<ResponsePayload> result =
          client.invoke("tool.name", new RequestPayload("hello"), ResponsePayload.class);

      assertEquals(7, result.getData().value);
    }
  }

  @Test
  void subscribeStreamDelegatesToTransport() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());
    AtomicReference<String> captured = new AtomicReference<>();

    Mockito
        .when(transport.postJson(eq("/mcp/session"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"SESSION_OPENED\",\"data\":{\"sessionId\":\"s-1\"}}"
                );
    client.openSession(null);
    client.subscribeStream(captured::set);

    verify(transport).getSse(eq("/mcp/stream"), any());
  }

  @Test
  void customPathsAreRespected() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    Mockito
        .when(transport.postJson(eq("/api/mcp/invoke"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"OK\",\"message\":\"done\",\"data\":{}}"
                );
    Mockito
        .when(transport.postJson(eq("/api/mcp/session"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"SESSION_OPENED\",\"data\":{\"sessionId\":\"s-2\"}}"
                );
    Mockito
        .when(transport.getJson("/api/mcp/tools"))
        .thenReturn("{\"status\":\"success\",\"code\":\"TOOLS\",\"data\":[]}");
    Mockito
        .when(transport.getJson("/api/mcp/governance/audit"))
        .thenReturn("{\"status\":\"success\",\"code\":\"AUDIT\",\"data\":{\"events\":[]}}");

    McpClient client =
        new McpClient(
            "client-1",
            transport,
            new ObjectMapper(),
            "/api/mcp/invoke",
            "/api/mcp/stream",
            "/api/mcp/session",
            "/api/mcp/tools",
            "/api/mcp/governance/audit");

    Mockito
        .when(transport.postJson(eq("/api/mcp/session"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"SESSION_OPENED\",\"data\":{"
                + "\"sessionId\":\"s-2\",\"protocol\":{\"invoke\":\"/api/mcp/invoke\",\"stream\":\"/api/mcp/stream\",\"discovery\":\"/api/mcp/tools\",\"governance\":\"/api/mcp/governance/audit\"},"
                + "\"tools\":[{\"name\":\"tool.name\"}]}}"
                );
    client.openSession(null);

    try (MockedStatic<Span> spanMock = Mockito.mockStatic(Span.class)) {
      spanMock.when(Span::current).thenReturn(null);
      client.invoke("tool.name", new RequestPayload("hello"), ResponsePayload.class);
    }
    client.subscribeStream(null, __ -> {});
    client.openSession(null);
    client.discoverTools();
    client.fetchGovernanceReport(null);

    verify(transport).postJson(eq("/api/mcp/invoke"), any());
    verify(transport).getSse(eq("/api/mcp/stream"), any());
    verify(transport).postJson(eq("/api/mcp/session"), any());
    verify(transport).getJson("/api/mcp/tools");
    verify(transport).getJson("/api/mcp/governance/audit");
  }

  @Test
  void findToolByCapabilityReturnsMatch() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());
    Mockito
        .when(transport.postJson(eq("/mcp/session"), any()))
        .thenReturn(
            "{\"status\":\"success\",\"code\":\"SESSION_OPENED\",\"data\":{\"sessionId\":\"s-1\","
                + "\"tools\":[{\"name\":\"tool.name\",\"capabilities\":[\"translate\"]}]}}"
                );

    client.openSession(null);

    assertTrue(client.findToolByCapability("translate").isPresent());
    assertTrue(client.findToolByCapability("unknown").isEmpty());
  }

  private static class RequestPayload {
    private final String value;

    RequestPayload(String value) {
      this.value = value;
    }
  }

  private static class ResponsePayload {
    private int value;
  }
}
