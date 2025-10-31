package com.example.mcp.framework.api;

import com.example.mcp.common.Envelopes;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import java.util.List;
import java.util.Optional;

public interface McpServer {

    SessionOpenResponse openSession(SessionOpenRequest request);

    List<ToolDescriptor> listTools();

    Optional<ToolDescriptor> describeTool(String name);

    <I, O> Envelopes.ResponseEnvelope<O> invoke(Envelopes.RequestEnvelope<I> request, Class<O> responseType);

    GovernanceReport governanceReport();
}
