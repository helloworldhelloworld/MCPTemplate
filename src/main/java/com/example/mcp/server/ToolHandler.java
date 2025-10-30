package com.example.mcp.server;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

/**
 * Functional interface used by the in-memory server to execute tool logic.
 */
public interface ToolHandler<I, O> {

    StdResponse<O> handle(Context context, I input);
}
