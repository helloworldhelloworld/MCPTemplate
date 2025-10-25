package com.example.mcp.client.interceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class HmacAuthInterceptor implements Interceptor {
  private final String clientId;
  private final String secret;

  public HmacAuthInterceptor(String clientId, String secret) {
    this.clientId = clientId;
    this.secret = secret;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request original = chain.request();
    if (!"POST".equalsIgnoreCase(original.method())) {
      return chain.proceed(original);
    }

    RequestBody body = original.body();
    String payload = "";
    if (body != null) {
      Buffer buffer = new Buffer();
      body.writeTo(buffer);
      payload = buffer.readString(StandardCharsets.UTF_8);
    }

    String timestamp = String.valueOf(System.currentTimeMillis());
    String signature;
    try {
      signature = sign(timestamp + "\n" + payload);
    } catch (Exception ex) {
      throw new IOException("Failed to calculate HMAC", ex);
    }

    Request signed =
        original
            .newBuilder()
            .header("X-MCP-ClientId", clientId)
            .header("X-MCP-Timestamp", timestamp)
            .header("X-MCP-Signature", signature)
            .method(original.method(), body)
            .build();
    return chain.proceed(signed);
  }

  private String sign(String message) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
  }
}
