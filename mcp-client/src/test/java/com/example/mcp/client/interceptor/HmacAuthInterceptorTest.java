package com.example.mcp.client.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HmacAuthInterceptorTest {

  @Test
  void addsSignatureHeadersForPostRequests() throws IOException {
    HmacAuthInterceptor interceptor = new HmacAuthInterceptor("client-1", "secret");
    Request original =
        new Request.Builder()
            .url("http://localhost/mcp/invoke")
            .post(RequestBody.create("{\"a\":1}", MediaType.get("application/json")))
            .build();
    Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
    Mockito.when(chain.request()).thenReturn(original);
    Mockito.when(chain.proceed(Mockito.any()))
        .thenAnswer(
            invocation -> {
              Request signed = invocation.getArgument(0);
              assertEquals("client-1", signed.header("X-MCP-ClientId"));
              assertNotNull(signed.header("X-MCP-Timestamp"));
              assertNotNull(signed.header("X-MCP-Signature"));
              return new Response.Builder()
                  .request(signed)
                  .protocol(Protocol.HTTP_1_1)
                  .code(200)
                  .message("OK")
                  .body(ResponseBody.create("{}", MediaType.get("application/json")))
                  .build();
            });

    interceptor.intercept(chain);
  }

  @Test
  void skipsNonPostRequests() throws IOException {
    HmacAuthInterceptor interceptor = new HmacAuthInterceptor("client-1", "secret");
    Request original = new Request.Builder().url("http://localhost/mcp/stream").get().build();
    Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
    Mockito.when(chain.request()).thenReturn(original);
    Mockito.when(chain.proceed(original)).thenReturn(buildResponse(original));

    interceptor.intercept(chain);

    Mockito.verify(chain).proceed(original);
  }

  private Response buildResponse(Request request) {
    return new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(ResponseBody.create("", MediaType.get("text/plain")))
        .build();
  }
}
