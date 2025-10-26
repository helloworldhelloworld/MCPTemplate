package com.example.mcp.client.event;

@FunctionalInterface
public interface McpEventListener {
  void onEvent(McpEvent event);
}
