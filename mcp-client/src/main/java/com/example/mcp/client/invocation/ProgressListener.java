package com.example.mcp.client.invocation;

import com.example.mcp.common.Envelopes.StreamEventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Listener for server-sent progress notifications.
 */
public interface ProgressListener {

  void onEvent(StreamEventEnvelope<JsonNode> event);

  default void onError(Throwable error) {}
}
