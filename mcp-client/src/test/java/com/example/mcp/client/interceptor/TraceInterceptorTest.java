package com.example.mcp.client.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TraceInterceptorTest {

  @Test
  void injectsTraceparentHeader() throws IOException {
    OpenTelemetrySdk sdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().build())
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();
    GlobalOpenTelemetry.resetForTest();
    GlobalOpenTelemetry.set(sdk);

    TraceInterceptor interceptor = new TraceInterceptor();
    Request request =
        new Request.Builder()
            .url("http://localhost/mcp/invoke")
            .post(RequestBody.create("{}", MediaType.get("application/json")))
            .build();
    Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
    Mockito.when(chain.request()).thenReturn(request);
    AtomicReference<Request> captured = new AtomicReference<>();
    Mockito.when(chain.proceed(Mockito.any()))
        .thenAnswer(
            invocation -> {
              Request updated = invocation.getArgument(0);
              captured.set(updated);
              return new Response.Builder()
                  .request(updated)
                  .protocol(Protocol.HTTP_1_1)
                  .code(200)
                  .message("OK")
                  .body(ResponseBody.create("{}", MediaType.get("application/json")))
                  .build();
            });

    Response response = interceptor.intercept(chain);

    assertEquals(200, response.code());
    assertNotNull(captured.get());
    assertNotNull(captured.get().header("traceparent"));
    GlobalOpenTelemetry.resetForTest();
  }
}
