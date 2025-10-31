package com.example.mcp.framework.api;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

@FunctionalInterface
public interface ToolHandler<I, O> {
    StdResponse<O> handle(Context context, I input);
}
