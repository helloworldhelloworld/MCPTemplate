package com.example.mcp.client.runtime;

import com.example.mcp.client.config.RouteConfig;
import com.example.mcp.client.event.McpEvent;
import com.example.mcp.client.event.McpEventBus;
import com.example.mcp.common.StdResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class McpRouteClient {
  private static final String ATTRIBUTE_ROUTE = "route";
  private static final String ATTRIBUTE_CORRELATION_ID = "correlationId";

  private final McpClientEnvironment environment;
  private final McpEventBus eventBus;

  public McpRouteClient(McpClientEnvironment environment) {
    this.environment = Objects.requireNonNull(environment, "environment");
    this.eventBus = environment.getEventBus();
  }

  public CompletionStage<StdResponse<?>> invoke(String routeName, Object requestPayload) {
    RouteConfig routeConfig = findRoute(routeName);
    String requestEvent = routeConfig.resolveRequestEvent(routeName);
    String responseEvent = routeConfig.resolveResponseEvent(routeName);
    String errorEvent = routeConfig.resolveErrorEvent(routeName);
    String correlationId = UUID.randomUUID().toString();

    CompletableFuture<StdResponse<?>> future = new CompletableFuture<>();
    McpEventBus.Subscription responseSubscription =
        eventBus.register(
            responseEvent,
            event -> {
              if (!correlationId.equals(event.getAttribute(ATTRIBUTE_CORRELATION_ID))) {
                return;
              }
              @SuppressWarnings("unchecked")
              StdResponse<?> response = (StdResponse<?>) event.getPayload();
              future.complete(response);
            });
    McpEventBus.Subscription errorSubscription =
        eventBus.register(
            errorEvent,
            event -> {
              if (!correlationId.equals(event.getAttribute(ATTRIBUTE_CORRELATION_ID))) {
                return;
              }
              Object payload = event.getPayload();
              if (payload instanceof Throwable) {
                future.completeExceptionally((Throwable) payload);
              } else {
                future.completeExceptionally(
                    new IllegalStateException("Unexpected error payload: " + payload));
              }
            });

    future.whenComplete(
        (response, throwable) -> {
          responseSubscription.close();
          errorSubscription.close();
        });

    Map<String, Object> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_ROUTE, routeName);
    attributes.put(ATTRIBUTE_CORRELATION_ID, correlationId);
    eventBus.publish(new McpEvent(requestEvent, requestPayload, attributes));
    return future;
  }

  public StdResponse<?> invokeBlocking(String routeName, Object requestPayload) throws Exception {
    return invoke(routeName, requestPayload).toCompletableFuture().get();
  }

  public <T> CompletionStage<StdResponse<T>> invoke(
      String routeName, Object requestPayload, Class<T> responseType) {
    return invoke(routeName, requestPayload)
        .thenApply(response -> ensureResponseType(response, responseType));
  }

  private RouteConfig findRoute(String routeName) {
    RouteConfig routeConfig = environment.getConfig().getRoutes().get(routeName);
    if (routeConfig == null) {
      throw new IllegalArgumentException("Route not configured: " + routeName);
    }
    return routeConfig;
  }

  private <T> StdResponse<T> ensureResponseType(StdResponse<?> response, Class<T> responseType) {
    Object data = response.getData();
    if (data == null || responseType.isInstance(data)) {
      @SuppressWarnings("unchecked")
      StdResponse<T> typed = (StdResponse<T>) response;
      return typed;
    }
    throw new IllegalStateException(
        "Response data type mismatch. Expected "
            + responseType.getName()
            + " but got "
            + data.getClass().getName());
  }
}
