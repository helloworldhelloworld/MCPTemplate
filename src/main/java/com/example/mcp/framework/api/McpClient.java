package com.example.mcp.framework.api;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import java.util.List;
import java.util.Optional;

public interface McpClient {

    SessionOpenResponse openSession(String locale);

    List<ToolDescriptor> listTools();

    Optional<ToolDescriptor> describeTool(String name);

    <I, O> Envelopes.ResponseEnvelope<O> invoke(String toolName, I payload, Class<O> responseType);

    Context getSessionContext();
}
