package com.example.mcp.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import org.junit.jupiter.api.Test;

class QaAnswerInvokeHandlerTest {

  private final QaAnswerInvokeHandler handler = new QaAnswerInvokeHandler();

  @Test
  void handleReturnsStubAnswer() {
    Context context = new Context();
    QaAnswerInvokeHandler.QaRequest request = new QaAnswerInvokeHandler.QaRequest();
    request.setQuestion("What is MCP?");
    request.setContext("context");

    StdResponse<QaAnswerInvokeHandler.QaResponse> response = handler.handle(context, request);

    assertEquals("QA_ANSWERED", response.getCode());
    assertEquals("Answer generated", response.getMessage());
    QaAnswerInvokeHandler.QaResponse data = response.getData();
    assertNotNull(data);
    assertEquals("Stub answer to: What is MCP?", data.getAnswer());
    assertEquals(0.42, data.getConfidence());
    assertEquals(18L, context.getUsage().getLatencyMs());
  }
}
