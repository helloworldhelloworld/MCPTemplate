package com.example.mcp.client.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class McpClientConfigLoader {
  private McpClientConfigLoader() {}

  public static McpClientConfig load(Path path) throws IOException {
    try (InputStream inputStream = Files.newInputStream(path)) {
      return load(inputStream);
    }
  }

  public static McpClientConfig load(InputStream inputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(inputStream, McpClientConfig.class);
  }
}
