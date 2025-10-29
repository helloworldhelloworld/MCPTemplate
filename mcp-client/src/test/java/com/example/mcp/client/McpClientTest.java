package com.example.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mcp.client.invocation.InvocationLogger;
import com.example.mcp.client.invocation.InvocationOptions;
import com.example.mcp.client.invocation.ProgressListener;
import com.example.mcp.client.invocation.SamplingResult;
import com.example.mcp.client.springai.SpringAiMcpClientBridge;
import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes.RequestEnvelope;
import com.example.mcp.common.Envelopes.StreamEventEnvelope;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

class McpClientTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void openSessionCachesTools() {
    SpringAiMcpClientBridge bridge = Mockito.mock(SpringAiMcpClientBridge.class);
    SessionOpenResponse session = new SessionOpenResponse();
    session.setSessionId("session-1");
    ToolDescriptor descriptor = new ToolDescriptor();
    descriptor.setName("demo-tool");
    session.setTools(List.of(descriptor));
    StdResponse<SessionOpenResponse> response = StdResponse.success("SESSION", "ok", session);
    when(bridge.openSession(any())).thenReturn(response);

    McpClient client = new McpClient("client-1", bridge, mapper);
    StdResponse<SessionOpenResponse> actual = client.openSession(null);

    assertEquals("session-1", actual.getData().getSessionId());
    assertTrue(client.findTool("demo-tool").isPresent());
  }

  @Test
  void discoverToolsPopulatesCache() {
    SpringAiMcpClientBridge bridge = Mockito.mock(SpringAiMcpClientBridge.class);
    ToolDescriptor descriptor = new ToolDescriptor();
    descriptor.setName("demo-tool");
    StdResponse<List<ToolDescriptor>> response = StdResponse.success("TOOLS", "ok", List.of(descriptor));
    when(bridge.discoverTools()).thenReturn(response);

    McpClient client = new McpClient("client-1", bridge, mapper);
    StdResponse<List<ToolDescriptor>> actual = client.discoverTools();

    assertEquals(1, actual.getData().size());
    assertTrue(client.findToolByCapability("missing").isEmpty());
    assertTrue(client.findTool("demo-tool").isPresent());
  }

  @Test
  void invokeConvertsResponsePayload() throws Exception {
    SpringAiMcpClientBridge bridge = Mockito.mock(SpringAiMcpClientBridge.class);
    McpClient client = new McpClient("client-1", bridge, mapper);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode data = mapper.readTree("{\"value\":42}");
    when(bridge.invoke(any(RequestEnvelope.class)))
        .thenReturn(StdResponse.success("OK", "done", data));

    try (MockedStatic<Span> spanMock = Mockito.mockStatic(Span.class)) {
      Span span = Mockito.mock(Span.class);
      SpanContext context = Mockito.mock(SpanContext.class);
      spanMock.when(Span::current).thenReturn(span);
      when(span.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(span);
      when(span.getSpanContext()).thenReturn(context);
      when(context.isValid()).thenReturn(true);
      when(context.getTraceId()).thenReturn("trace-id");

      StdResponse<ResponsePayload> response =
          client.invoke("demo", new RequestPayload("hi"), ResponsePayload.class);
      assertEquals(42, response.getData().value);
    }
  }

  @Test
  void invokeSupportsSampling() throws Exception {
    SpringAiMcpClientBridge bridge = Mockito.mock(SpringAiMcpClientBridge.class);
    McpClient client = new McpClient("client-1", bridge, mapper);
    JsonNode data = mapper.readTree("{\"value\":21}");
    when(bridge.invoke(any(RequestEnvelope.class)))
        .thenReturn(StdResponse.success("OK", "done", data));

    SamplingResult<ResponsePayload> result =
        client.invokeWithOptions(
            "demo",
            new RequestPayload("hi"),
            ResponsePayload.class,
            InvocationOptions.<RequestPayload>builder().samples(3).build());

    assertEquals(3, result.samples().size());
    verify(bridge, times(3)).invoke(any(RequestEnvelope.class));
  }

  @Test
  void fetchGovernanceDelegatesToBridge() {
    SpringAiMcpClientBridge bridge = Mockito.mock(SpringAiMcpClientBridge.class);
    GovernanceReport report = new GovernanceReport();
    report.setEvents(List.of(new GovernanceReport.Event()));
    when(bridge.governance("req-1")).thenReturn(StdResponse.success("AUDIT", "ok", report));

    McpClient client = new McpClient("client-1", bridge, mapper);
    StdResponse<GovernanceReport> response = client.fetchGovernanceReport("req-1");

    assertEquals(1, response.getData().getEvents().size());
    verify(bridge).governance("req-1");
  }

  @Test
  void progressListenerFiltersByRequestId() {
    SpringAiMcpClientBridge bridge = Mockito.mock(SpringAiMcpClientBridge.class);
    McpClient client = new McpClient("client-1", bridge, mapper);
    StreamEventEnvelope<JsonNode> matching = new StreamEventEnvelope<>();
    com.example.mcp.common.Envelopes.Response<JsonNode> response =
        new com.example.mcp.common.Envelopes.Response<>();
    response.setData(mapper.createObjectNode().put("requestId", "req-1"));
    matching.setResponse(response);
    StreamEventEnvelope<JsonNode> other = new StreamEventEnvelope<>();
    com.example.mcp.common.Envelopes.Response<JsonNode> otherResponse =
        new com.example.mcp.common.Envelopes.Response<>();
    otherResponse.setData(mapper.createObjectNode().put("requestId", "req-2"));
    other.setResponse(otherResponse);

    when(bridge.notifications(null)).thenReturn(Flux.just(matching, other));

    AtomicReference<StreamEventEnvelope<JsonNode>> captured = new AtomicReference<>();
    ProgressListener listener = new ProgressListener() {
      @Override
      public void onEvent(StreamEventEnvelope<JsonNode> event) {
        captured.set(event);
      }

      @Override
      public void onError(Throwable error) {
        throw new AssertionError(error);
      }
    };

    RequestPayload payload = new RequestPayload("hi");
    InvocationOptions<RequestPayload> options =
        InvocationOptions.<RequestPayload>builder().progressListener(listener).build();

    when(bridge.invoke(any(RequestEnvelope.class)))
        .thenReturn(StdResponse.success("OK", "done", mapper.createObjectNode()));

    try {
      client.invokeWithOptions("demo", payload, ResponsePayload.class, options);
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }

    assertNotNull(captured.get());
    assertEquals("req-1", captured.get().getResponse().getData().path("requestId").asText());
  }

  @Test
  void registerLoggerInvokesCallbacks() throws Exception {
    SpringAiMcpClientBridge bridge = Mockito.mock(SpringAiMcpClientBridge.class);
    McpClient client = new McpClient("client-1", bridge, mapper);
    JsonNode data = mapper.readTree("{\"value\":10}");
    when(bridge.invoke(any(RequestEnvelope.class)))
        .thenReturn(StdResponse.success("OK", "done", data));

    InvocationLogger logger = Mockito.mock(InvocationLogger.class);
    client.registerLogger(logger);
    client.invoke("demo", new RequestPayload("hi"), ResponsePayload.class);

    verify(logger).onRequest(Mockito.eq("demo"), any(Context.class), Mockito.any());
    verify(logger).onResponse(Mockito.eq("demo"), any(Context.class), any(StdResponse.class));
  }

  private record RequestPayload(String text) {}

  private static class ResponsePayload {
    int value;
  }
}
