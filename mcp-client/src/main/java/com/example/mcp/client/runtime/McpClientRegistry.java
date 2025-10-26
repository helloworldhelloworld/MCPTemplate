package com.example.mcp.client.runtime;

import com.example.mcp.client.McpClient;
import com.example.mcp.client.config.McpClientConfig;
import com.example.mcp.client.config.ServerConfig;
import com.example.mcp.client.config.TransportFactory;
import com.example.mcp.client.transport.Transport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpClientRegistry {
  private final McpClientConfig config;
  private final TransportFactory transportFactory;
  private final Map<String, McpClient> clients = new ConcurrentHashMap<>();

  public McpClientRegistry(McpClientConfig config, TransportFactory transportFactory) {
    this.config = config;
    this.transportFactory = transportFactory;
    initializeClients();
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
      Transport transport = transportFactory.create(serverName, serverConfig);
      ObjectMapper mapper = new ObjectMapper();
      McpClient client = new McpClient(config.getClientId(), transport, mapper);
      clients.put(serverName, client);
    }
  }
}
