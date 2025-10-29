package com.example.mcp.client.runtime;

import com.example.mcp.client.McpClient;
import com.example.mcp.client.config.McpClientConfig;
import com.example.mcp.client.config.ServerConfig;
import com.example.mcp.client.springai.SpringAiMcpClientAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpClientRegistry implements AutoCloseable {
  private final McpClientConfig config;
  private final Map<String, McpClient> clients = new ConcurrentHashMap<>();

  public McpClientRegistry(McpClientConfig config) {
    this.config = config;
    initializeClients();
  }

  public McpClientRegistry(McpClientConfig config, Map<String, McpClient> initialClients) {
    this.config = config;
    this.clients.putAll(initialClients);
  }

  public McpClient getClient(String serverName) {
    McpClient client = clients.get(serverName);
    if (client == null) {
      throw new IllegalArgumentException("Unknown MCP server: " + serverName);
    }
    return client;
  }

  public Map<String, McpClient> getAll() {
    return Collections.unmodifiableMap(clients);
  }

  private void initializeClients() {
    for (Map.Entry<String, ServerConfig> entry : config.getServers().entrySet()) {
      String serverName = entry.getKey();
      ServerConfig serverConfig = entry.getValue();
      clients.put(serverName, createClient(serverName, serverConfig));
    }
  }

  private McpClient createClient(String serverName, ServerConfig serverConfig) {
    if (serverConfig.getBaseUrl() == null || serverConfig.getBaseUrl().isBlank()) {
      throw new IllegalArgumentException(
          "Spring AI MCP client requires baseUrl for server " + serverName);
    }
    ObjectMapper mapper = new ObjectMapper();
    SpringAiMcpClientAdapter bridge =
        new SpringAiMcpClientAdapter(config.getClientId(), serverConfig.getBaseUrl());
    return new McpClient(config.getClientId(), bridge, mapper);
  }

  @Override
  public void close() {
    clients.values().forEach(
        client -> {
          try {
            client.close();
          } catch (Exception ignored) {
          }
        });
    clients.clear();
  }
}
