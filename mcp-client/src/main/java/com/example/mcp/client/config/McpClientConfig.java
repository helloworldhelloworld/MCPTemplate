package com.example.mcp.client.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class McpClientConfig {
  private String clientId = "mcp-client";
  private String defaultServer;
  private Map<String, ServerConfig> servers = new LinkedHashMap<>();
  private Map<String, RouteConfig> routes = new LinkedHashMap<>();

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getDefaultServer() {
    return defaultServer;
  }

  public void setDefaultServer(String defaultServer) {
    this.defaultServer = defaultServer;
  }

  public Map<String, ServerConfig> getServers() {
    return servers;
  }

  public void setServers(Map<String, ServerConfig> servers) {
    this.servers = servers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(servers);
  }

  public Map<String, RouteConfig> getRoutes() {
    return routes;
  }

  public void setRoutes(Map<String, RouteConfig> routes) {
    this.routes = routes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(routes);
  }

  public String resolveDefaultServer() {
    if (defaultServer != null) {
      return defaultServer;
    }
    return servers.keySet().stream().findFirst().orElse(null);
  }
}
