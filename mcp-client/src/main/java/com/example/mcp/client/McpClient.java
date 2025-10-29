package com.example.mcp.client;

import com.example.mcp.client.invocation.ElicitationStrategy;
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
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

public class McpClient implements AutoCloseable {
  private final String clientId;
  private final SpringAiMcpClientBridge bridge;
  private final ObjectMapper objectMapper;
  private final Map<String, ToolDescriptor> toolCatalog = new ConcurrentHashMap<>();
  private final List<InvocationLogger> loggers = new CopyOnWriteArrayList<>();
  private final NotificationHub notificationHub;

  public McpClient(String clientId, String baseUrl) {
    this(clientId, baseUrl, new ObjectMapper());
  }

  public McpClient(String clientId, String baseUrl, ObjectMapper objectMapper) {
    this(clientId, new SpringAiMcpClientBridge(clientId, baseUrl), objectMapper);
  }

  public McpClient(String clientId, SpringAiMcpClientBridge bridge, ObjectMapper objectMapper) {
    this.clientId = Objects.requireNonNull(clientId, "clientId");
    this.bridge = Objects.requireNonNull(bridge, "bridge");
    this.objectMapper = configure(Objects.requireNonNull(objectMapper, "objectMapper"));
    this.notificationHub = new NotificationHub();
  }

  private static ObjectMapper configure(ObjectMapper mapper) {
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  public AutoCloseable registerLogger(InvocationLogger logger) {
    InvocationLogger nonNull = Objects.requireNonNull(logger, "logger");
    loggers.add(nonNull);
    return () -> loggers.remove(nonNull);
  }

  public AutoCloseable registerProgressListener(ProgressListener listener) {
    return notificationHub.register(Objects.requireNonNull(listener, "listener"));
  }

  public StdResponse<SessionOpenResponse> openSession(SessionOpenRequest request) {
    SessionOpenRequest payload = request != null ? request : new SessionOpenRequest();
    if (payload.getClientId() == null) {
      payload.setClientId(clientId);
    }
    StdResponse<SessionOpenResponse> response = bridge.openSession(payload);
    applySessionMetadata(response.getData());
    return response;
  }

  public StdResponse<List<ToolDescriptor>> discoverTools() {
    StdResponse<List<ToolDescriptor>> response = bridge.discoverTools();
    cacheTools(response.getData());
    return response;
  }

  public StdResponse<GovernanceReport> fetchGovernanceReport(String requestId) {
    return bridge.governance(requestId);
  }

  public <TRequest, TResponse> StdResponse<TResponse> invoke(
      String toolName, TRequest requestPayload, Class<TResponse> responseType) throws Exception {
    SamplingResult<TResponse> result =
        invokeWithOptions(toolName, requestPayload, responseType, InvocationOptions.<TRequest>builder().build());
    StdResponse<TResponse> primary = result.primary();
    if (primary == null) {
      throw new IllegalStateException("Invocation produced no samples");
    }
    return primary;
  }

  public <TRequest, TResponse> SamplingResult<TResponse> invokeWithOptions(
      String toolName,
      TRequest requestPayload,
      Class<TResponse> responseType,
      InvocationOptions<TRequest> options)
      throws Exception {
    InvocationOptions<TRequest> effective =
        options != null ? options : InvocationOptions.<TRequest>builder().build();
    List<StdResponse<TResponse>> samples = new ArrayList<>();
    for (int i = 0; i < effective.getSamples(); i++) {
      samples.add(invokeOnce(toolName, requestPayload, responseType, effective));
    }
    return new SamplingResult<>(samples);
  }

  private <TRequest, TResponse> StdResponse<TResponse> invokeOnce(
      String toolName,
      TRequest requestPayload,
      Class<TResponse> responseType,
      InvocationOptions<TRequest> options)
      throws Exception {
    TRequest current = requestPayload;
    StdResponse<TResponse> response = null;
    int attempts = 0;
    do {
      RequestEnvelope envelope = buildRequestEnvelope(toolName, current);
      AutoCloseable progressRegistration =
          registerScopedProgressListener(envelope.getContext(), options.getProgressListener().orElse(null));
      try {
        notifyLoggersRequest(toolName, envelope, current, options.getLogger().orElse(null));
        StdResponse<JsonNode> raw = bridge.invoke(envelope);
        response = convertResponse(raw, responseType);
        notifyLoggersResponse(toolName, envelope.getContext(), response, options.getLogger().orElse(null));
      } catch (Exception ex) {
        notifyLoggersError(toolName, envelope.getContext(), ex, options.getLogger().orElse(null));
        throw ex;
      } finally {
        if (progressRegistration != null) {
          try {
            progressRegistration.close();
          } catch (Exception ignored) {
          }
        }
      }

      if (!isClarification(response) || options.getElicitationStrategy().isEmpty()) {
        return response;
      }
      ElicitationStrategy<TRequest> strategy = options.getElicitationStrategy().get();
      TRequest next = strategy.refine(current, response, envelope.getContext());
      if (next == null || next.equals(current)) {
        return response;
      }
      current = next;
      attempts++;
    } while (attempts < options.getMaxElicitationRounds());
    return response;
  }

  public <TRequest, TResponse> CompletionStage<StdResponse<TResponse>> invokeAsync(
      String toolName, TRequest requestPayload, Class<TResponse> responseType) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return invoke(toolName, requestPayload, responseType);
          } catch (Exception ex) {
            throw new CompletionException(ex);
          }
        });
  }

  public void subscribeStream(Consumer<String> onEvent) {
    subscribeStream(null, onEvent);
  }

  public void subscribeStream(String path, Consumer<String> onEvent) {
    Objects.requireNonNull(onEvent, "onEvent");
    Flux<StreamEventEnvelope<JsonNode>> flux = bridge.notifications(null);
    flux.doOnNext(
            event -> {
              try {
                onEvent.accept(objectMapper.writeValueAsString(event));
              } catch (Exception ex) {
                throw new CompletionException(ex);
              }
            })
        .blockLast();
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

  @Override
  public void close() {
    notificationHub.dispose();
    bridge.close();
  }

  private <TResponse> StdResponse<TResponse> convertResponse(
      StdResponse<JsonNode> raw, Class<TResponse> responseType) throws Exception {
    if (raw == null) {
      return null;
    }
    StdResponse<TResponse> typed = new StdResponse<>();
    typed.setStatus(raw.getStatus());
    typed.setCode(raw.getCode());
    typed.setMessage(raw.getMessage());
    if (raw.getData() != null && responseType != null && !Void.class.equals(responseType)) {
      typed.setData(objectMapper.treeToValue(raw.getData(), responseType));
    }
    return typed;
  }

  private boolean isClarification(StdResponse<?> response) {
    return response != null && "clarify".equalsIgnoreCase(response.getStatus());
  }

  private RequestEnvelope buildRequestEnvelope(String toolName, Object requestPayload) {
    RequestEnvelope envelope = new RequestEnvelope();
    envelope.setTool(toolName);
    Context context = buildContext();
    envelope.setContext(context);
    envelope.setPayload(objectMapper.valueToTree(requestPayload));
    Span current = Span.current();
    if (current != null) {
      current.setAttribute("tool.name", toolName);
    }
    return envelope;
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

  private AutoCloseable registerScopedProgressListener(Context context, ProgressListener listener) {
    if (listener == null) {
      return () -> {};
    }
    String requestId = context != null ? context.getRequestId() : null;
    ProgressListener filtered =
        new ProgressListener() {
          @Override
          public void onEvent(StreamEventEnvelope<JsonNode> event) {
            JsonNode data = event.getResponse() != null ? event.getResponse().getData() : null;
            String eventRequestId = data != null ? data.path("requestId").asText(null) : null;
            if (requestId == null || requestId.equals(eventRequestId)) {
              listener.onEvent(event);
            }
          }

          @Override
          public void onError(Throwable error) {
            listener.onError(error);
          }
        };
    return notificationHub.register(filtered);
  }

  private void notifyLoggersRequest(
      String toolName, RequestEnvelope envelope, Object payload, InvocationLogger callLogger) {
    Context context = envelope.getContext();
    for (InvocationLogger logger : loggers) {
      try {
        logger.onRequest(toolName, context, payload);
      } catch (RuntimeException ignored) {
      }
    }
    if (callLogger != null) {
      try {
        callLogger.onRequest(toolName, context, payload);
      } catch (RuntimeException ignored) {
      }
    }
  }

  private void notifyLoggersResponse(
      String toolName, Context context, StdResponse<?> response, InvocationLogger callLogger) {
    for (InvocationLogger logger : loggers) {
      try {
        logger.onResponse(toolName, context, response);
      } catch (RuntimeException ignored) {
      }
    }
    if (callLogger != null) {
      try {
        callLogger.onResponse(toolName, context, response);
      } catch (RuntimeException ignored) {
      }
    }
  }

  private void notifyLoggersError(
      String toolName, Context context, Throwable error, InvocationLogger callLogger) {
    for (InvocationLogger logger : loggers) {
      try {
        logger.onError(toolName, context, error);
      } catch (RuntimeException ignored) {
      }
    }
    if (callLogger != null) {
      try {
        callLogger.onError(toolName, context, error);
      } catch (RuntimeException ignored) {
      }
    }
  }

  private void applySessionMetadata(SessionOpenResponse session) {
    if (session == null) {
      return;
    }
    cacheTools(session.getTools());
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

  private class NotificationHub {
    private final List<ProgressListener> listeners = new CopyOnWriteArrayList<>();
    private volatile Disposable subscription;

    AutoCloseable register(ProgressListener listener) {
      listeners.add(listener);
      ensureSubscribed();
      return () -> unregister(listener);
    }

    private synchronized void ensureSubscribed() {
      if (listeners.isEmpty()) {
        return;
      }
      if (subscription != null && !subscription.isDisposed()) {
        return;
      }
      subscription =
          bridge
              .notifications(null)
              .doOnError(this::notifyListenersError)
              .subscribe(
                  event -> {
                    for (ProgressListener listener : listeners) {
                      try {
                        listener.onEvent(event);
                      } catch (RuntimeException ignored) {
                      }
                    }
                  },
                  this::notifyListenersError);
    }

    private synchronized void unregister(ProgressListener listener) {
      listeners.remove(listener);
      if (listeners.isEmpty() && subscription != null) {
        subscription.dispose();
        subscription = null;
      }
    }

    private void notifyListenersError(Throwable error) {
      for (ProgressListener listener : listeners) {
        try {
          listener.onError(error);
        } catch (RuntimeException ignored) {
        }
      }
    }

    void dispose() {
      if (subscription != null) {
        subscription.dispose();
        subscription = null;
      }
      listeners.clear();
    }
  }
}
