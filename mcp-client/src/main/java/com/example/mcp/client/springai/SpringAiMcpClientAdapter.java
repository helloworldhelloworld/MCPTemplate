package com.example.mcp.client.springai;

import com.example.mcp.client.spi.McpFrameworkClient;
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
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.transport.http.HttpMcpClientTransport;

public class SpringAiMcpClientAdapter implements McpFrameworkClient {

  private final McpClient delegate;

  public SpringAiMcpClientAdapter(String clientId, String baseUrl) {
    this(buildClient(clientId, baseUrl));
  }

  public SpringAiMcpClientAdapter(McpClient delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public StdResponse<SessionOpenResponse> openSession(SessionOpenRequest request) {
    return block(delegate.openSession(request));
  }

  @Override
  public StdResponse<List<ToolDescriptor>> discoverTools() {
    return block(delegate.listTools());
  }

  @Override
  public StdResponse<JsonNode> invoke(RequestEnvelope requestEnvelope) {
    return block(delegate.invoke(requestEnvelope));
  }

  @Override
  public Flux<StreamEventEnvelope<JsonNode>> notifications(Duration timeout) {
    Flux<StreamEventEnvelope<JsonNode>> stream = delegate.notifications();
    return timeout != null ? stream.timeout(timeout) : stream;
  }

  @Override
  public StdResponse<GovernanceReport> governance(String requestId) {
    return block(delegate.governance(requestId));
  }

  @Override
  public void close() {
    delegate.close();
  }

  private static McpClient buildClient(String clientId, String baseUrl) {
    HttpMcpClientTransport transport =
        HttpMcpClientTransport.builder().baseUrl(baseUrl).build();
    return McpClient.builder().clientName(clientId).transport(transport).build();
  }

  private <T> T block(Mono<T> mono) {
    return mono.blockOptional().orElse(null);
  }
}
