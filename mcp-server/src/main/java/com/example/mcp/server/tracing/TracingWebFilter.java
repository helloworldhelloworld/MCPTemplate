package com.example.mcp.server.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive tracing filter that propagates the current trace context and annotates spans with
 * MCP specific metadata.
 */
@Component
public class TracingWebFilter implements WebFilter {

  private static final AttributeKey<String> TOOL_NAME = AttributeKey.stringKey("tool.name");
  private static final AttributeKey<String> CLIENT_ID = AttributeKey.stringKey("client.id");
  private static final AttributeKey<Long> LATENCY = AttributeKey.longKey("latency.ms");

  private final Tracer tracer = GlobalOpenTelemetry.getTracer("com.example.mcp.server");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    if (!request.getURI().getPath().startsWith("/mcp")) {
      return chain.filter(exchange);
    }

    Context parent =
        W3CTraceContextPropagator.getInstance()
            .extract(Context.current(), request, new ServerHttpRequestGetter());
    long start = System.currentTimeMillis();
    Span span =
        tracer
            .spanBuilder(request.getURI().getPath())
            .setParent(parent)
            .setSpanKind(SpanKind.SERVER)
            .startSpan();
    span.setAttribute(CLIENT_ID, request.getHeaders().getFirst("X-MCP-ClientId"));

    Scope scope = span.makeCurrent();
    return chain
        .filter(exchange)
        .doOnSuccess(
            ignored -> {
              Object toolName = exchange.getAttribute("mcp.tool.name");
              if (toolName == null) {
                toolName = request.getHeaders().getFirst("X-MCP-Tool");
              }
              if (toolName != null) {
                span.setAttribute(TOOL_NAME, toolName.toString());
              }
            })
        .doOnTerminate(() -> span.setAttribute(LATENCY, System.currentTimeMillis() - start))
        .doFinally(signalType -> {
          scope.close();
          span.end();
        });
  }

  private static class ServerHttpRequestGetter implements TextMapGetter<ServerHttpRequest> {
    @Override
    public Iterable<String> keys(ServerHttpRequest carrier) {
      return carrier.getHeaders().keySet();
    }

    @Override
    public String get(ServerHttpRequest carrier, String key) {
      return carrier.getHeaders().getFirst(key);
    }
  }
}
