package com.example.mcp.server.handler;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.ToolDescriptor;

public interface ToolHandler<TRequest, TResponse> {
  String getToolName();

  Class<TRequest> getRequestType();

  StdResponse<TResponse> handle(Context context, TRequest request) throws Exception;

  default ToolDescriptor describe() {
    ToolDescriptor descriptor = new ToolDescriptor();
    descriptor.setName(getToolName());
    descriptor.setTitle(getToolName());
    descriptor.setDescription("MCP tool");
    return descriptor;
  }
}
