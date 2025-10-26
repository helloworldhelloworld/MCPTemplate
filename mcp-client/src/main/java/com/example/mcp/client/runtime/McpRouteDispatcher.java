package com.example.mcp.client.runtime;

import com.example.mcp.client.McpClient;
import com.example.mcp.client.config.McpClientConfig;
import com.example.mcp.client.config.RouteConfig;
import com.example.mcp.client.event.McpEvent;
import com.example.mcp.client.event.McpEventBus;
import com.example.mcp.client.event.McpEventListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

public class McpRouteDispatcher implements AutoCloseable {
  private static final String ATTRIBUTE_ROUTE = "route";
  private static final String ATTRIBUTE_SERVER = "server";

  private final McpClientConfig config;
  private final McpClientRegistry registry;
  private final McpEventBus eventBus;
  private final ObjectMapper mapper;
  private final List<McpEventBus.Subscription> subscriptions = new ArrayList<>();

  public McpRouteDispatcher(McpClientConfig config, McpClientRegistry registry, McpEventBus eventBus) {
    this.config = config;
    this.registry = registry;
    this.eventBus = eventBus;
    this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public void start() {
    for (Map.Entry<String, RouteConfig> entry : config.getRoutes().entrySet()) {
      registerRoute(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void close() {
    subscriptions.forEach(
        subscription -> {
          try {
            subscription.close();
          } catch (Exception ignored) {
            // no-op
          }
        });
    subscriptions.clear();
  }

  private void registerRoute(String routeName, RouteConfig routeConfig) {
    String requestEvent = routeConfig.resolveRequestEvent(routeName);
    String responseEvent = routeConfig.resolveResponseEvent(routeName);
    String errorEvent = routeConfig.resolveErrorEvent(routeName);

    String serverName = resolveServer(routeConfig);
    McpClient client = registry.getClient(serverName);
    String requestTypeName = routeConfig.getRequestType();
    Class<?> responseType =
        resolveClass(routeConfig.getResponseType()).orElse(JsonNode.class);

    McpEventListener listener =
        event -> {
          Map<String, Object> attributes = buildAttributes(event, routeName, serverName);
          try {
            Object payload = maybeConvert(event.getPayload(), requestTypeName);
            @SuppressWarnings("unchecked")
            Class<Object> typedResponse = (Class<Object>) responseType;
            client
                .invokeAsync(routeConfig.getTool(), payload, typedResponse)
                .whenComplete(
                    (response, throwable) -> {
                      if (throwable != null) {
                        eventBus.publish(new McpEvent(errorEvent, unwrap(throwable), attributes));
                      } else {
                        eventBus.publish(new McpEvent(responseEvent, response, attributes));
                      }
                    });
          } catch (Throwable ex) {
            eventBus.publish(new McpEvent(errorEvent, unwrap(ex), attributes));
          }
        };

    subscriptions.add(eventBus.register(requestEvent, listener));
  }

  private Map<String, Object> buildAttributes(
      McpEvent requestEvent, String routeName, String serverName) {
    Map<String, Object> attributes = new HashMap<>(requestEvent.getAttributes());
    attributes.put(ATTRIBUTE_ROUTE, routeName);
    attributes.put(ATTRIBUTE_SERVER, serverName);
    return attributes;
  }

  private String resolveServer(RouteConfig routeConfig) {
    if (routeConfig.getServer() != null) {
      return routeConfig.getServer();
    }
    String defaultServer = config.resolveDefaultServer();
    if (defaultServer == null) {
      throw new IllegalStateException("No default server configured for route " + routeConfig.getTool());
    }
    return defaultServer;
  }

  private Object maybeConvert(Object payload, String requestType) {
    if (payload == null || requestType == null) {
      return payload;
    }
    try {
      Class<?> type = Class.forName(requestType);
      if (type.isInstance(payload)) {
        return payload;
      }
      return mapper.convertValue(payload, type);
    } catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException("Unknown request type " + requestType, ex);
    }
  }

  private java.util.Optional<Class<?>> resolveClass(String className) {
    if (className == null || className.isBlank()) {
      return java.util.Optional.empty();
    }
    try {
      return java.util.Optional.of(Class.forName(className));
    } catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException("Unable to resolve class " + className, ex);
    }
  }

  private Throwable unwrap(Throwable throwable) {
    if (throwable instanceof CompletionException && throwable.getCause() != null) {
      return throwable.getCause();
    }
    if (throwable instanceof RuntimeException && throwable.getCause() != null) {
      return throwable.getCause();
    }
    return throwable;
  }
}
