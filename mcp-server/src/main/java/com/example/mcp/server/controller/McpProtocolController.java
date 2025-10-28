package com.example.mcp.server.controller;

import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.example.mcp.server.handler.ToolHandler;
import com.example.mcp.server.handler.ToolRegistry;
import com.example.mcp.server.service.InvocationAuditService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpProtocolController {
  private final ToolRegistry toolRegistry;
  private final InvocationAuditService auditService;
  private final String serverName;
  private final String serverVersion;

  public McpProtocolController(
      ToolRegistry toolRegistry,
      InvocationAuditService auditService,
      @Value("${mcp.server.name:demo-mcp-server}") String serverName,
      @Value("${mcp.server.version:1.0.0}") String serverVersion) {
    this.toolRegistry = toolRegistry;
    this.auditService = auditService;
    this.serverName = serverName;
    this.serverVersion = serverVersion;
  }

  @PostMapping(path = "/mcp/session", consumes = MediaType.APPLICATION_JSON_VALUE)
  public StdResponse<SessionOpenResponse> openSession(
      @RequestBody(required = false) SessionOpenRequest request) {
    SessionOpenResponse response = new SessionOpenResponse();
    response.setSessionId(UUID.randomUUID().toString());
    response.setServerName(serverName);
    response.setServerVersion(serverVersion);
    response.setExpiresAt(OffsetDateTime.now().plusHours(1));
    response.setTools(describeTools());
    return StdResponse.success("SESSION_OPENED", "会话建立成功", response);
  }

  @GetMapping(path = "/mcp/tools", produces = MediaType.APPLICATION_JSON_VALUE)
  public StdResponse<List<ToolDescriptor>> discoverTools() {
    return StdResponse.success("TOOLS", "能力发现成功", describeTools());
  }

  @GetMapping(path = "/mcp/governance/audit/{requestId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public StdResponse<GovernanceReport> audit(@PathVariable("requestId") String requestId) {
    GovernanceReport report = auditService.findByRequestId(requestId);
    return StdResponse.success("AUDIT", "治理审计", report);
  }

  private List<ToolDescriptor> describeTools() {
    return toolRegistry.getHandlers().values().stream()
        .map(ToolHandler::describe)
        .collect(Collectors.toList());
  }
}
