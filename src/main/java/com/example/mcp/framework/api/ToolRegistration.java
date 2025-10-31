package com.example.mcp.framework.api;

import com.example.mcp.common.protocol.ToolDescriptor;

public record ToolRegistration<I, O>(ToolDescriptor descriptor,
                                     Class<I> inputType,
                                     ToolHandler<I, O> handler,
                                     Class<O> outputType) {
}
