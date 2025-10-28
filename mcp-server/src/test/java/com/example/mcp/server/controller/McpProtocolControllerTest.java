package com.example.mcp.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.example.mcp.server.handler.ToolRegistry;
import com.example.mcp.server.handler.TranslationInvokeHandler;
import com.example.mcp.server.service.InvocationAuditService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class McpProtocolControllerTest {

  private InvocationAuditService auditService;
  private ToolRegistry registry;
  private McpProtocolController controller;

  @BeforeEach
  void setUp() {
    auditService = new InvocationAuditService();
    registry = new ToolRegistry(List.of(new TranslationInvokeHandler()));
    controller = new McpProtocolController(registry, auditService, "demo", "1.2.3");
  }

  @Test
  void openSessionReturnsServerMetadataAndTools() {
    SessionOpenRequest request = new SessionOpenRequest();
    request.setClientId("client-1");

    StdResponse<SessionOpenResponse> response = controller.openSession(request);

    assertEquals("SESSION_OPENED", response.getCode());
    assertNotNull(response.getData().getSessionId());
    assertEquals("demo", response.getData().getServerName());
    assertNotNull(response.getData().getProtocol());
    assertEquals(
        McpProtocolController.INVOKE_PATH, response.getData().getProtocol().getInvoke());
    assertEquals(1, response.getData().getTools().size());
    ToolDescriptor descriptor = response.getData().getTools().get(0);
    assertEquals("mcp.translation.invoke", descriptor.getName());
    assertEquals("schema/translation-request.json", descriptor.getInputSchema());
  }

  @Test
  void discoverToolsMatchesRegistryDescriptors() {
    StdResponse<List<ToolDescriptor>> response = controller.discoverTools();

    assertEquals("TOOLS", response.getCode());
    assertEquals(1, response.getData().size());
    assertEquals("翻译工具", response.getData().get(0).getTitle());
  }

  @Test
  void auditReturnsRecordedEvents() {
    com.example.mcp.common.protocol.InvocationAuditRecord record =
        new com.example.mcp.common.protocol.InvocationAuditRecord();
    record.setRequestId("req-1");
    record.setTool("mcp.translation.invoke");
    auditService.record(record);

    StdResponse<GovernanceReport> response = controller.audit("req-1");

    assertEquals("AUDIT", response.getCode());
    assertEquals(1, response.getData().getEvents().size());
    assertEquals("req-1", response.getData().getEvents().get(0).getRequestId());
  }
}
