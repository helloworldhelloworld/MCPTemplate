package com.example.mcp.server.handler;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {
  private final Map<String, ToolHandler<?, ?>> handlers = new ConcurrentHashMap<>();

  public ToolRegistry(Collection<ToolHandler<?, ?>> toolHandlers) {
    toolHandlers.forEach(handler -> handlers.put(handler.getToolName(), handler));
  }

  public Optional<ToolHandler<?, ?>> find(String toolName) {
    return Optional.ofNullable(handlers.get(toolName));
  }

  public Map<String, ToolHandler<?, ?>> getHandlers() {
    return Collections.unmodifiableMap(handlers);
  }
}
