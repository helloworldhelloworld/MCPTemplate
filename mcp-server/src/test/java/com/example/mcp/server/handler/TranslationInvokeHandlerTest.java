package com.example.mcp.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import org.junit.jupiter.api.Test;

class TranslationInvokeHandlerTest {

  private final TranslationInvokeHandler handler = new TranslationInvokeHandler();

  @Test
  void handleUppercasesTextAndPopulatesUsage() {
    Context context = new Context();
    TranslationInvokeHandler.TranslationRequest request = new TranslationInvokeHandler.TranslationRequest();
    request.setText("hello");
    request.setSourceLanguage("en");
    request.setTargetLanguage("fr");

    StdResponse<TranslationInvokeHandler.TranslationResponse> response = handler.handle(context, request);

    assertEquals("TRANSLATED", response.getCode());
    assertEquals("Translation completed", response.getMessage());
    TranslationInvokeHandler.TranslationResponse data = response.getData();
    assertNotNull(data);
    assertEquals("HELLO", data.getTranslatedText());
    assertEquals("en", data.getSourceLanguage());
    assertEquals("fr", data.getTargetLanguage());
    assertNotNull(context.getUsage());
    assertEquals(12L, context.getUsage().getLatencyMs());
  }
}
