package com.example.mcp.client.transport;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

public class SdkTransport implements Transport {
  private final Object sdkClient;
  private final Method postJson;
  private final Method getSse;
  private final Method getJson;

  public SdkTransport(Object sdkClient) {
    this.sdkClient = Objects.requireNonNull(sdkClient, "sdkClient");
    try {
      Class<?> sdkClass = sdkClient.getClass();
      postJson = sdkClass.getMethod("postJson", String.class, String.class);
      getSse = sdkClass.getMethod("getSse", String.class, Consumer.class);
      Method getJsonCandidate = null;
      try {
        getJsonCandidate = sdkClass.getMethod("getJson", String.class);
      } catch (NoSuchMethodException ignored) {
      }
      this.getJson = getJsonCandidate;
    } catch (NoSuchMethodException ex) {
      throw new IllegalArgumentException("SDK client missing required methods", ex);
    }
  }

  public static SdkTransport autoDetect() {
    try {
      Class<?> loader = Class.forName("com.modelcontextprotocol.client.DefaultTransport");
      Object instance = loader.getDeclaredConstructor().newInstance();
      return new SdkTransport(instance);
    } catch (Exception ex) {
      throw new IllegalStateException("modelcontextprotocol:java-sdk not available", ex);
    }
  }

  @Override
  public String postJson(String path, String json) throws Exception {
    return (String) postJson.invoke(sdkClient, path, json);
  }

  @Override
  public String getJson(String path) throws Exception {
    if (getJson != null) {
      return (String) getJson.invoke(sdkClient, path);
    }
    return (String) postJson.invoke(sdkClient, path, null);
  }

  @Override
  public void getSse(String path, Consumer<String> onEvent) throws Exception {
    getSse.invoke(sdkClient, path, onEvent);
  }
}
