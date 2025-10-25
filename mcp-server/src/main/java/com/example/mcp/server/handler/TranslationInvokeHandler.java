package com.example.mcp.server.handler;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import org.springframework.stereotype.Component;

@Component
public class TranslationInvokeHandler
    implements ToolHandler<TranslationInvokeHandler.TranslationRequest, TranslationInvokeHandler.TranslationResponse> {

  @Override
  public String getToolName() {
    return "mcp.translation.invoke";
  }

  @Override
  public Class<TranslationRequest> getRequestType() {
    return TranslationRequest.class;
  }

  @Override
  public StdResponse<TranslationResponse> handle(Context context, TranslationRequest request) {
    TranslationResponse response = new TranslationResponse();
    response.setSourceLanguage(request.getSourceLanguage());
    response.setTargetLanguage(request.getTargetLanguage());
    response.setOriginalText(request.getText());
    response.setTranslatedText(request.getText() == null ? "" : request.getText().toUpperCase());
    Context.Usage usage = new Context.Usage();
    usage.setLatencyMs(12L);
    context.setUsage(usage);
    return StdResponse.success("TRANSLATED", "Translation completed", response);
  }

  public static class TranslationRequest {
    private String text;
    private String sourceLanguage;
    private String targetLanguage;

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    public String getSourceLanguage() {
      return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
      this.sourceLanguage = sourceLanguage;
    }

    public String getTargetLanguage() {
      return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
      this.targetLanguage = targetLanguage;
    }
  }

  public static class TranslationResponse {
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;

    public String getOriginalText() {
      return originalText;
    }

    public void setOriginalText(String originalText) {
      this.originalText = originalText;
    }

    public String getTranslatedText() {
      return translatedText;
    }

    public void setTranslatedText(String translatedText) {
      this.translatedText = translatedText;
    }

    public String getSourceLanguage() {
      return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
      this.sourceLanguage = sourceLanguage;
    }

    public String getTargetLanguage() {
      return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
      this.targetLanguage = targetLanguage;
    }
  }
}
