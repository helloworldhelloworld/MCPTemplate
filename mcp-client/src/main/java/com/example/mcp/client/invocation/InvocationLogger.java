package com.example.mcp.client.invocation;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

/**
 * Hook for observing request/response lifecycles during tool invocation.
 */
public interface InvocationLogger {

  default void onRequest(String toolName, Context context, Object payload) {}

  default void onResponse(String toolName, Context context, StdResponse<?> response) {}

  default void onError(String toolName, Context context, Throwable error) {}
}
