package com.example.mcp.client;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes.RequestEnvelope;
import com.example.mcp.common.Envelopes.ResponseEnvelope;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.ProtocolDescriptor;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.example.mcp.client.transport.Transport;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.opentelemetry.api.trace.Span;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class McpClient {
  private static final String DEFAULT_INVOKE_PATH = "/mcp/invoke";
  private static final String DEFAULT_STREAM_PATH = "/mcp/stream";
  private static final String DEFAULT_SESSION_PATH = "/mcp/session";
  private static final String DEFAULT_DISCOVERY_PATH = "/mcp/tools";
  private static final String DEFAULT_GOVERNANCE_PATH = "/mcp/governance/audit";

  private final String clientId;
  private final Transport transport;
  private final ObjectMapper objectMapper;
  private volatile String invokePath;
  private volatile String streamPath;
  private volatile String sessionPath;
  private volatile String discoveryPath;
  private volatile String governancePath;
  private volatile ProtocolDescriptor negotiatedProtocol;
  private final Map<String, ToolDescriptor> toolCatalog = new ConcurrentHashMap<>();

  public McpClient(String clientId, Transport transport) {
    this(clientId, transport, new ObjectMapper());
  }

  public McpClient(String clientId, Transport transport, ObjectMapper objectMapper) {
    this(
        clientId,
        transport,
        objectMapper,
        DEFAULT_INVOKE_PATH,
        DEFAULT_STREAM_PATH,
        DEFAULT_SESSION_PATH,
        DEFAULT_DISCOVERY_PATH,
        DEFAULT_GOVERNANCE_PATH);
  }

  public McpClient(
      String clientId,
      Transport transport,
      ObjectMapper objectMapper,
      String invokePath,
      String streamPath) {
    this(
        clientId,
        transport,
        objectMapper,
        invokePath,
        streamPath,
        DEFAULT_SESSION_PATH,
        DEFAULT_DISCOVERY_PATH,
        DEFAULT_GOVERNANCE_PATH);
  }

  public McpClient(
      String clientId,
      Transport transport,
      ObjectMapper objectMapper,
      String invokePath,
      String streamPath,
      String sessionPath,
      String discoveryPath,
      String governancePath) {
    this.clientId = Objects.requireNonNull(clientId, "clientId");
    this.transport = Objects.requireNonNull(transport, "transport");
    this.objectMapper = configure(Objects.requireNonNull(objectMapper, "objectMapper"));
    this.invokePath = defaultIfBlank(invokePath, DEFAULT_INVOKE_PATH);
    this.streamPath = defaultIfBlank(streamPath, DEFAULT_STREAM_PATH);
    this.sessionPath = defaultIfBlank(sessionPath, DEFAULT_SESSION_PATH);
    this.discoveryPath = defaultIfBlank(discoveryPath, DEFAULT_DISCOVERY_PATH);
    this.governancePath = defaultIfBlank(governancePath, DEFAULT_GOVERNANCE_PATH);
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

  public StdResponse<SessionOpenResponse> openSession(SessionOpenRequest request) throws Exception {
    SessionOpenRequest payload = request != null ? request : new SessionOpenRequest();
    if (payload.getClientId() == null) {
      payload.setClientId(clientId);
    }
    String json = objectMapper.writeValueAsString(payload);
    String responseJson = transport.postJson(sessionPath, json);
    JavaType stdType =
        objectMapper
            .getTypeFactory()
            .constructParametricType(StdResponse.class, SessionOpenResponse.class);
    StdResponse<SessionOpenResponse> response = objectMapper.readValue(responseJson, stdType);
    applySessionMetadata(response.getData());
    return response;
  }

  public StdResponse<List<ToolDescriptor>> discoverTools() throws Exception {
    String responseJson = transport.getJson(discoveryPath);
    JavaType listType =
        objectMapper
            .getTypeFactory()
            .constructCollectionType(List.class, ToolDescriptor.class);
    JavaType stdType = objectMapper.getTypeFactory().constructParametricType(StdResponse.class, listType);
    StdResponse<List<ToolDescriptor>> response = objectMapper.readValue(responseJson, stdType);
    cacheTools(response.getData());
    return response;
  }

  public StdResponse<GovernanceReport> fetchGovernanceReport(String requestId) throws Exception {
    String path = governancePath;
    if (requestId != null && !requestId.isBlank()) {
      path = governancePath.endsWith("/") ? governancePath + requestId : governancePath + "/" + requestId;
    }
    String responseJson = transport.getJson(path);
    JavaType stdType =
        objectMapper
            .getTypeFactory()
            .constructParametricType(StdResponse.class, GovernanceReport.class);
    return objectMapper.readValue(responseJson, stdType);
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

  public Optional<ToolDescriptor> findTool(String toolName) {
    if (toolName == null || toolName.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(toolCatalog.get(toolName));
  }

  public Optional<ToolDescriptor> findToolByCapability(String capability) {
    if (capability == null || capability.isBlank()) {
      return Optional.empty();
    }
    return toolCatalog.values().stream()
        .filter(descriptor -> descriptor.getCapabilities() != null)
        .filter(descriptor -> descriptor.getCapabilities().contains(capability))
        .findFirst();
  }

  public List<ToolDescriptor> getCachedTools() {
    return Collections.unmodifiableList(new ArrayList<>(toolCatalog.values()));
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

  private void applySessionMetadata(SessionOpenResponse session) {
    if (session == null) {
      return;
    }
    cacheTools(session.getTools());
    ProtocolDescriptor descriptor = session.getProtocol();
    if (descriptor != null) {
      this.negotiatedProtocol = descriptor;
      this.sessionPath = defaultIfBlank(descriptor.getSession(), this.sessionPath);
      this.invokePath = defaultIfBlank(descriptor.getInvoke(), this.invokePath);
      this.streamPath = defaultIfBlank(descriptor.getStream(), this.streamPath);
      this.discoveryPath = defaultIfBlank(descriptor.getDiscovery(), this.discoveryPath);
      this.governancePath = defaultIfBlank(descriptor.getGovernance(), this.governancePath);
    }
  }

  private void cacheTools(List<ToolDescriptor> descriptors) {
    if (descriptors == null || descriptors.isEmpty()) {
      return;
    }
    for (ToolDescriptor descriptor : descriptors) {
      if (descriptor != null && descriptor.getName() != null) {
        toolCatalog.put(descriptor.getName(), descriptor);
      }
    }
  }
}
