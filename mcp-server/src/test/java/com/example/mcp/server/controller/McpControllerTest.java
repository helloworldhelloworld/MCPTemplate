package com.example.mcp.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes.RequestEnvelope;
import com.example.mcp.common.Envelopes.ResponseEnvelope;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.InvocationAuditRecord;
import com.example.mcp.server.handler.ToolHandler;
import com.example.mcp.server.handler.ToolRegistry;
import com.example.mcp.server.handler.TranslationInvokeHandler;
import com.example.mcp.server.service.InvocationAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class McpControllerTest {

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Mock private ToolRegistry toolRegistry;
  @Mock private InvocationAuditService auditService;

  private McpController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new McpController(toolRegistry, mapper, auditService);
  }

  @Test
  void invokePopulatesContextAndUiCardForTranslation() throws Exception {
    TranslationInvokeHandler handler = new TranslationInvokeHandler();
    when(toolRegistry.find("mcp.translation.invoke")).thenReturn(Optional.of(handler));

    RequestEnvelope envelope = new RequestEnvelope();
    envelope.setTool("mcp.translation.invoke");
    Context context = new Context();
    envelope.setContext(context);
    JsonNode payload =
        mapper
            .createObjectNode()
            .put("sourceText", "hello")
            .put("targetLocale", "fr-FR");
    envelope.setPayload(payload);

    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.addHeader("X-MCP-ClientId", "client-1");

    ResponseEntity<?> response = controller.invoke(envelope, servletRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    ResponseEnvelope<?> body = (ResponseEnvelope<?>) response.getBody();
    assertNotNull(body);
    assertEquals("mcp.translation.invoke", body.getTool());
    assertNotNull(body.getContext().getRequestId());
    assertEquals("client-1", body.getContext().getClientId());
    assertNotNull(body.getUiCard());
    assertEquals("Translation Result", body.getUiCard().getTitle());
    assertTrue(body.getContext().getTimestamp().isBefore(OffsetDateTime.now().plusSeconds(1)));

    ArgumentCaptor<InvocationAuditRecord> captor =
        ArgumentCaptor.forClass(InvocationAuditRecord.class);
    verify(auditService).record(captor.capture());
    assertEquals("mcp.translation.invoke", captor.getValue().getTool());
    assertEquals("success", captor.getValue().getStatus());
  }

  @Test
  void invokeReturnsClarificationCardWhenTargetMissing() throws Exception {
    TranslationInvokeHandler handler = new TranslationInvokeHandler();
    when(toolRegistry.find("mcp.translation.invoke")).thenReturn(Optional.of(handler));

    RequestEnvelope envelope = new RequestEnvelope();
    envelope.setTool("mcp.translation.invoke");
    Context context = new Context();
    envelope.setContext(context);
    JsonNode payload = mapper.createObjectNode().put("sourceText", "你好");
    envelope.setPayload(payload);

    MockHttpServletRequest servletRequest = new MockHttpServletRequest();

    ResponseEntity<?> response = controller.invoke(envelope, servletRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    ResponseEnvelope<?> body = (ResponseEnvelope<?>) response.getBody();
    assertNotNull(body);
    assertEquals("clarify", body.getResponse().getStatus());
    assertNotNull(body.getUiCard());
    assertEquals("澄清请求", body.getUiCard().getTitle());
    assertEquals("请选择目标语言，例如 zh-CN、en-US", body.getUiCard().getBody());
    assertTrue(body.getUiCard().getActions().containsKey("选择中文"));

    ArgumentCaptor<InvocationAuditRecord> captor =
        ArgumentCaptor.forClass(InvocationAuditRecord.class);
    verify(auditService).record(captor.capture());
    assertEquals("clarify", captor.getValue().getStatus());
  }

  @Test
  void invokeReturnsBadRequestForUnknownTool() {
    when(toolRegistry.find("unknown.tool")).thenReturn(Optional.empty());

    RequestEnvelope envelope = new RequestEnvelope();
    envelope.setTool("unknown.tool");
    MockHttpServletRequest request = new MockHttpServletRequest();

    ResponseEntity<?> response = controller.invoke(envelope, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void invokeHandlesExceptionsFromHandler() throws Exception {
    @SuppressWarnings("unchecked")
    ToolHandler<Object, Object> handler = org.mockito.Mockito.mock(ToolHandler.class);
    when(handler.getRequestType()).thenReturn(Object.class);
    when(toolRegistry.find("mcp.problem.tool")).thenReturn(Optional.of(handler));
    when(handler.handle(org.mockito.Mockito.any(), org.mockito.Mockito.isNull()))
        .thenThrow(new IOException("boom"));

    RequestEnvelope envelope = new RequestEnvelope();
    envelope.setTool("mcp.problem.tool");
    MockHttpServletRequest request = new MockHttpServletRequest();

    ResponseEntity<?> response = controller.invoke(envelope, request);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  void invokeConvertsNullPayload() throws Exception {
    @SuppressWarnings("unchecked")
    ToolHandler<String, String> handler = org.mockito.Mockito.mock(ToolHandler.class);
    when(handler.getRequestType()).thenReturn(String.class);
    when(handler.handle(org.mockito.Mockito.any(), org.mockito.Mockito.isNull()))
        .thenReturn(StdResponse.success("OK", "done", "data"));
    when(toolRegistry.find("tool.null")).thenReturn(Optional.of(handler));

    RequestEnvelope envelope = new RequestEnvelope();
    envelope.setTool("tool.null");
    Context context = new Context();
    context.setRequestId(UUID.randomUUID().toString());
    context.setTimestamp(OffsetDateTime.now());
    envelope.setContext(context);

    MockHttpServletRequest request = new MockHttpServletRequest();

    ResponseEntity<?> response = controller.invoke(envelope, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    ResponseEnvelope<?> body = (ResponseEnvelope<?>) response.getBody();
    assertNotNull(body);
    assertEquals("tool.null", body.getTool());
  }

  @Test
  void streamEventsCompletesAndEmitsHeartbeat() throws Exception {
    SseEmitter emitter = controller.streamEvents();
    CountDownLatch completed = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();

    emitter.onCompletion(completed::countDown);
    emitter.onError(
        ex -> {
          error.set(ex);
          completed.countDown();
        });

    assertTrue(completed.await(1, TimeUnit.SECONDS));
    assertNull(error.get());
  }
}
