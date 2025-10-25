package com.example.mcp.client;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes.RequestEnvelope;
import com.example.mcp.common.Envelopes.ResponseEnvelope;
import com.example.mcp.common.StdResponse;
import com.example.mcp.client.transport.Transport;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.opentelemetry.api.trace.Span;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public class McpClient {
  private final String clientId;
  private final Transport transport;
  private final ObjectMapper objectMapper;

  public McpClient(String clientId, Transport transport, ObjectMapper objectMapper) {
    this.clientId = clientId;
    this.transport = transport;
    this.objectMapper = configure(objectMapper);
  }

  public McpClient(String clientId, Transport transport) {
    this(clientId, transport, configure(new ObjectMapper()));
  }

  private static ObjectMapper configure(ObjectMapper mapper) {
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  public <TRequest, TResponse> StdResponse<TResponse> invoke(
      String toolName, TRequest requestPayload, Class<TResponse> responseType) throws Exception {
    RequestEnvelope envelope = new RequestEnvelope();
    envelope.setTool(toolName);
    Context context = buildContext();
    envelope.setContext(context);
    envelope.setPayload(objectMapper.valueToTree(requestPayload));

    Span current = Span.current();
    if (current != null) {
      current.setAttribute("tool.name", toolName);
    }

    String payload = objectMapper.writeValueAsString(envelope);
    String responseJson = transport.postJson("/mcp/invoke", payload);
    JsonNode rootNode = objectMapper.readTree(responseJson);
    if (rootNode.has("status")) {
      JavaType stdType =
          objectMapper
              .getTypeFactory()
              .constructParametricType(StdResponse.class, responseType);
      return objectMapper.readValue(responseJson, stdType);
    }
    JavaType envelopeType =
        objectMapper
            .getTypeFactory()
            .constructParametricType(ResponseEnvelope.class, responseType);
    ResponseEnvelope<TResponse> responseEnvelope =
        objectMapper.readValue(responseJson, envelopeType);
    return responseEnvelope.getResponse();
  }

  public void subscribeStream(String path, Consumer<String> onEvent) throws Exception {
    transport.getSse(path, onEvent);
  }

  private Context buildContext() {
    Context context = new Context();
    context.setClientId(clientId);
    context.setRequestId(UUID.randomUUID().toString());
    context.setTimestamp(OffsetDateTime.now());
    Span span = Span.current();
    if (span != null && span.getSpanContext().isValid()) {
      context.setTraceId(span.getSpanContext().getTraceId());
    }
    return context;
  }
}
