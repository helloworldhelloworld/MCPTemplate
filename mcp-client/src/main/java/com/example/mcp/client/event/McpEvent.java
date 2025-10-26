package com.example.mcp.client.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class McpEvent {
  private final String name;
  private final Object payload;
  private final Map<String, Object> attributes;

  public McpEvent(String name, Object payload) {
    this(name, payload, Collections.emptyMap());
  }

  public McpEvent(String name, Object payload, Map<String, Object> attributes) {
    this.name = name;
    this.payload = payload;
    if (attributes == null || attributes.isEmpty()) {
      this.attributes = Collections.emptyMap();
    } else {
      this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }
  }

  public String getName() {
    return name;
  }

  public Object getPayload() {
    return payload;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public Object getAttribute(String key) {
    return attributes.get(key);
  }
}
