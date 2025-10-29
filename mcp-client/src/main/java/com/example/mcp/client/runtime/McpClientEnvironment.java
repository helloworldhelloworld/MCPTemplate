package com.example.mcp.client.runtime;

import com.example.mcp.client.config.McpClientConfig;
import com.example.mcp.client.config.McpClientConfigLoader;
import com.example.mcp.client.event.McpEventBus;
import java.io.IOException;
import java.nio.file.Path;

public class McpClientEnvironment implements AutoCloseable {
  private final McpClientConfig config;
  private final McpEventBus eventBus;
  private final McpClientRegistry registry;
  private final McpRouteDispatcher dispatcher;

  public McpClientEnvironment(McpClientConfig config) {
    this(config, new McpClientRegistry(config));
  }

  public McpClientEnvironment(McpClientConfig config, McpClientRegistry registry) {
    this.config = config;
    this.eventBus = new McpEventBus();
    this.registry = registry;
    this.dispatcher = new McpRouteDispatcher(config, registry, eventBus);
  }

  public static McpClientEnvironment load(Path configPath) throws IOException {
    McpClientConfig config = McpClientConfigLoader.load(configPath);
    McpClientEnvironment environment = new McpClientEnvironment(config);
    environment.start();
    return environment;
  }

  public void start() {
    dispatcher.start();
  }

  public McpEventBus getEventBus() {
    return eventBus;
  }

  public McpClientRegistry getRegistry() {
    return registry;
  }

  public McpClientConfig getConfig() {
    return config;
  }

  @Override
  public void close() {
    dispatcher.close();
    registry.close();
  }
}
