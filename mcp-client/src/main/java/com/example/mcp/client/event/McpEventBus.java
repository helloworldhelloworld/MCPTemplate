package com.example.mcp.client.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class McpEventBus {
  private final Map<String, CopyOnWriteArrayList<McpEventListener>> listeners =
      new ConcurrentHashMap<>();

  public Subscription register(String eventName, McpEventListener listener) {
    listeners.computeIfAbsent(eventName, key -> new CopyOnWriteArrayList<>()).add(listener);
    return new Subscription(eventName, listener);
  }

  public void publish(String eventName, Object payload) {
    publish(new McpEvent(eventName, payload));
  }

  public void publish(McpEvent event) {
    List<McpEventListener> subscribers =
        listeners.getOrDefault(event.getName(), new CopyOnWriteArrayList<>());
    for (McpEventListener listener : subscribers) {
      listener.onEvent(event);
    }
  }

  public final class Subscription implements AutoCloseable {
    private final String eventName;
    private final McpEventListener listener;
    private volatile boolean active = true;

    private Subscription(String eventName, McpEventListener listener) {
      this.eventName = eventName;
      this.listener = listener;
    }

    @Override
    public void close() {
      if (!active) {
        return;
      }
      CopyOnWriteArrayList<McpEventListener> subscribers = listeners.get(eventName);
      if (subscribers != null) {
        subscribers.remove(listener);
      }
      active = false;
    }
  }
}
