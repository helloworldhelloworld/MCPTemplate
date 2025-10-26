package com.example.mcp.client.transport;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface Transport {
  String postJson(String path, String json) throws Exception;

  void getSse(String path, Consumer<String> onEvent) throws Exception;

  default CompletionStage<String> postJsonAsync(String path, String json) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return postJson(path, json);
          } catch (Exception ex) {
            throw new CompletionException(ex);
          }
        });
  }

  default CompletionStage<Void> getSseAsync(String path, Consumer<String> onEvent) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            getSse(path, onEvent);
          } catch (Exception ex) {
            throw new CompletionException(ex);
          }
        });
  }
}
