package com.example.mcp.server.handler;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

public interface ToolHandler<TRequest, TResponse> {
  String getToolName();

  Class<TRequest> getRequestType();

  StdResponse<TResponse> handle(Context context, TRequest request) throws Exception;
}
