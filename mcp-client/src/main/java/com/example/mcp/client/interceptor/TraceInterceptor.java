package com.example.mcp.client.interceptor;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TraceInterceptor implements Interceptor {
  private final Tracer tracer = GlobalOpenTelemetry.getTracer("com.example.mcp.client");

  @Override
  public Response intercept(Chain chain) throws java.io.IOException {
    Request original = chain.request();
    Span span =
        tracer
            .spanBuilder(original.method() + " " + original.url().encodedPath())
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();
    try (Scope scope = span.makeCurrent()) {
      Request.Builder builder = original.newBuilder();
      W3CTraceContextPropagator.getInstance()
          .inject(Context.current(), builder, new RequestBuilderSetter());
      Response response = chain.proceed(builder.build());
      span.setAttribute("http.status_code", response.code());
      return response;
    } finally {
      span.end();
    }
  }

  private static class RequestBuilderSetter implements TextMapSetter<Request.Builder> {
    @Override
    public void set(Request.Builder carrier, String key, String value) {
      if (carrier != null && key != null && value != null) {
        carrier.header(key, value);
      }
    }
  }
}
