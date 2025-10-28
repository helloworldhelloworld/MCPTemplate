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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.Objects;

public class McpClient {
  private static final String DEFAULT_INVOKE_PATH = "/mcp/invoke";
  private static final String DEFAULT_STREAM_PATH = "/mcp/stream";

  private final String clientId;
  private final Transport transport;
  private final ObjectMapper objectMapper;
  private final String invokePath;
  private final String streamPath;

  public McpClient(String clientId, Transport transport) {
    this(clientId, transport, new ObjectMapper());
  }

  public McpClient(String clientId, Transport transport, ObjectMapper objectMapper) {
    this(clientId, transport, objectMapper, DEFAULT_INVOKE_PATH, DEFAULT_STREAM_PATH);
  }

  public McpClient(
      String clientId,
      Transport transport,
      ObjectMapper objectMapper,
      String invokePath,
      String streamPath) {
    this.clientId = Objects.requireNonNull(clientId, "clientId");
    this.transport = Objects.requireNonNull(transport, "transport");
    this.objectMapper = configure(Objects.requireNonNull(objectMapper, "objectMapper"));
    this.invokePath = defaultIfBlank(invokePath, DEFAULT_INVOKE_PATH);
    this.streamPath = defaultIfBlank(streamPath, DEFAULT_STREAM_PATH);
  }

  private static ObjectMapper configure(ObjectMapper mapper) {
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  private static String defaultIfBlank(String value, String defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value;
  }

  public <TRequest, TResponse> StdResponse<TResponse> invoke(
      String toolName, TRequest requestPayload, Class<TResponse> responseType) throws Exception {
    String payload = serializeRequest(toolName, requestPayload);
    String responseJson = transport.postJson(invokePath, payload);
    return deserializeResponse(responseJson, responseType);
  }

  public <TRequest, TResponse> CompletionStage<StdResponse<TResponse>> invokeAsync(
      String toolName, TRequest requestPayload, Class<TResponse> responseType) {
    try {
      String payload = serializeRequest(toolName, requestPayload);
      return transport
          .postJsonAsync(invokePath, payload)
          .thenApply(
              responseJson -> {
                try {
                  return deserializeResponse(responseJson, responseType);
                } catch (Exception ex) {
                  throw new CompletionException(ex);
                }
              });
    } catch (Exception ex) {
      CompletableFuture<StdResponse<TResponse>> failed = new CompletableFuture<>();
      failed.completeExceptionally(ex);
      return failed;
    }
  }

  private <TRequest> String serializeRequest(String toolName, TRequest requestPayload)
      throws Exception {
    RequestEnvelope envelope = new RequestEnvelope();
    envelope.setTool(toolName);
    Context context = buildContext();
    envelope.setContext(context);
    envelope.setPayload(objectMapper.valueToTree(requestPayload));

    Span current = Span.current();
    if (current != null) {
      current.setAttribute("tool.name", toolName);
    }

    return objectMapper.writeValueAsString(envelope);
  }

  private <TResponse> StdResponse<TResponse> deserializeResponse(
      String responseJson, Class<TResponse> responseType) throws Exception {
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

  public void subscribeStream(Consumer<String> onEvent) throws Exception {
    subscribeStream(null, onEvent);
  }

  public void subscribeStream(String path, Consumer<String> onEvent) throws Exception {
    transport.getSse(defaultIfBlank(path, streamPath), onEvent);
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
