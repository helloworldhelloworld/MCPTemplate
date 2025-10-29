package com.example.mcp.client.invocation;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

/**
 * Strategy that refines the request payload when the server asks for clarification.
 */
@FunctionalInterface
public interface ElicitationStrategy<TRequest> {

  TRequest refine(TRequest currentRequest, StdResponse<?> clarification, Context context);
}
