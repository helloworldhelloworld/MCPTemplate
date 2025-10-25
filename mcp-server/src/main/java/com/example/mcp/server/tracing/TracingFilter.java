package com.example.mcp.server.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.extension.trace.propagation.W3CTraceContextPropagator;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TracingFilter extends OncePerRequestFilter {
  private static final AttributeKey<String> TOOL_NAME = AttributeKey.stringKey("tool.name");
  private static final AttributeKey<String> CLIENT_ID = AttributeKey.stringKey("client.id");
  private static final AttributeKey<Long> LATENCY = AttributeKey.longKey("latency.ms");
  private final Tracer tracer = GlobalOpenTelemetry.getTracer("com.example.mcp.server");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/mcp");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, java.io.IOException {
    Context parentContext =
        W3CTraceContextPropagator.getInstance()
            .extract(Context.current(), request, new HttpServletRequestGetter());
    long start = System.currentTimeMillis();
    Span span =
        tracer
            .spanBuilder(request.getRequestURI())
            .setSpanKind(SpanKind.SERVER)
            .setParent(parentContext)
            .startSpan();

    span.setAttribute(CLIENT_ID, request.getHeader("X-MCP-ClientId"));
    try (Scope scope = span.makeCurrent()) {
      filterChain.doFilter(request, response);
    } finally {
      Object toolName = request.getAttribute("mcp.tool.name");
      if (toolName instanceof String) {
        span.setAttribute(TOOL_NAME, (String) toolName);
      }
      span.setAttribute(LATENCY, System.currentTimeMillis() - start);
      span.end();
    }
  }

  private static class HttpServletRequestGetter implements TextMapGetter<HttpServletRequest> {
    @Override
    public Iterable<String> keys(HttpServletRequest carrier) {
      return carrier.getHeaderNames() == null
          ? java.util.Collections.emptyList()
          : java.util.Collections.list(carrier.getHeaderNames());
    }

    @Override
    public String get(HttpServletRequest carrier, String key) {
      return carrier.getHeader(key);
    }
  }
}
