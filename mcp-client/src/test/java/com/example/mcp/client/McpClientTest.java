package com.example.mcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

import com.example.mcp.client.transport.Transport;
import com.example.mcp.common.StdResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class McpClientTest {

  @Test
  void invokeParsesStdResponse() throws Exception {
    Transport transport = Mockito.mock(Transport.class);
    McpClient client = new McpClient("client-1", transport, new ObjectMapper());
    AtomicReference<String> payloadHolder = new AtomicReference<>();

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

    client.subscribeStream("/mcp/stream", captured::set);

    Mockito.verify(transport).getSse(eq("/mcp/stream"), any());
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
