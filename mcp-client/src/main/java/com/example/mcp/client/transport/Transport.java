package com.example.mcp.client.transport;

import java.util.function.Consumer;

public interface Transport {
  String postJson(String path, String json) throws Exception;

  void getSse(String path, Consumer<String> onEvent) throws Exception;
}
