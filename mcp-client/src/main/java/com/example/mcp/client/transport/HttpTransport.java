package com.example.mcp.client.transport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpTransport implements Transport {
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private final String baseUrl;
  private final OkHttpClient client;

  public HttpTransport(String baseUrl, List<Interceptor> interceptors) {
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    if (interceptors != null) {
      interceptors.forEach(builder::addInterceptor);
    }
    this.client = builder.build();
  }

  public HttpTransport(String baseUrl) {
    this(baseUrl, null);
  }

  @Override
  public String postJson(String path, String json) throws Exception {
    Request request =
        new Request.Builder()
            .url(baseUrl + path)
            .post(RequestBody.create(json, JSON))
            .build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IllegalStateException("HTTP error: " + response.code());
      }
      return Objects.requireNonNull(response.body()).string();
    }
  }

  @Override
  public void getSse(String path, Consumer<String> onEvent) throws Exception {
    Request request =
        new Request.Builder()
            .url(baseUrl + path)
            .get()
            .addHeader("Accept", "text/event-stream")
            .build();
    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IllegalStateException("HTTP error: " + response.code());
      }
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.isEmpty()) {
            onEvent.accept(line);
          }
        }
      }
    }
  }
}
