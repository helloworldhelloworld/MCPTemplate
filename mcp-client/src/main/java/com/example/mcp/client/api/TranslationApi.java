package com.example.mcp.client.api;

import com.example.mcp.client.McpClient;
import com.example.mcp.common.StdResponse;

public class TranslationApi {
  private final McpClient client;

  public TranslationApi(McpClient client) {
    this.client = client;
  }

  public StdResponse<TranslationResponse> translate(
      String text, String sourceLanguage, String targetLanguage) throws Exception {
    TranslationRequest request = new TranslationRequest();
    request.setText(text);
    request.setSourceLanguage(sourceLanguage);
    request.setTargetLanguage(targetLanguage);
    return client.invoke("mcp.translation.invoke", request, TranslationResponse.class);
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
