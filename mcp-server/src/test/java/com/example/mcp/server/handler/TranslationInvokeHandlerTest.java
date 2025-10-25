package com.example.mcp.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.server.handler.TranslationInvokeHandler.TranslationRequest;
import com.example.mcp.server.handler.TranslationInvokeHandler.TranslationResponse;
import org.junit.jupiter.api.Test;

class TranslationInvokeHandlerTest {

  private final TranslationInvokeHandler handler = new TranslationInvokeHandler();

  @Test
  void handleShouldUppercaseTextAndPopulateContextUsage() {
    Context context = new Context();
    TranslationRequest request = new TranslationRequest();
    request.setText("hello world");
    request.setSourceLanguage("en");
    request.setTargetLanguage("fr");

    StdResponse<TranslationResponse> response = handler.handle(context, request);

    assertEquals("success", response.getStatus());
    TranslationResponse body = response.getData();
    assertNotNull(body);
    assertEquals("hello world", body.getOriginalText());
    assertEquals("HELLO WORLD", body.getTranslatedText());
    assertEquals("en", body.getSourceLanguage());
    assertEquals("fr", body.getTargetLanguage());
    assertNotNull(context.getUsage());
    assertEquals(Long.valueOf(12L), context.getUsage().getLatencyMs());
  }
}
