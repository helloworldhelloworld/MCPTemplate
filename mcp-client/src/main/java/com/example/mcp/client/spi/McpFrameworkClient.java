package com.example.mcp.client.spi;

import com.example.mcp.common.Envelopes.RequestEnvelope;
import com.example.mcp.common.Envelopes.StreamEventEnvelope;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import reactor.core.publisher.Flux;

public interface McpFrameworkClient extends AutoCloseable {

  StdResponse<SessionOpenResponse> openSession(SessionOpenRequest request);

  StdResponse<List<ToolDescriptor>> discoverTools();

  StdResponse<JsonNode> invoke(RequestEnvelope requestEnvelope);

  Flux<StreamEventEnvelope<JsonNode>> notifications(Duration timeout);

  StdResponse<GovernanceReport> governance(String requestId);

  @Override
  void close();
}
