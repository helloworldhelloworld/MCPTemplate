package com.example.mcp.server.handler;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    Map<String, String> metadata = ensureMetadata(context);

    if (request == null || !StringUtils.hasText(request.getText())) {
      metadata.put("clarify.missingField", "text");
      metadata.put("clarify.prompt", "请补充需要翻译的原文内容");
      metadata.put("clarify.examples", "例如：你好，世界");
      return StdResponse.clarify("TRANSLATION_TEXT_REQUIRED", "需要提供原文后才能继续翻译");
    }

    if (!StringUtils.hasText(request.getTargetLanguage())) {
      metadata.put("clarify.missingField", "targetLanguage");
      metadata.put("clarify.prompt", "请选择目标语言，例如 zh-CN、en-US");
      metadata.put("clarify.options", "zh-CN,en-US,ja-JP");
      return StdResponse.clarify("TRANSLATION_TARGET_REQUIRED", "需要确认目标语言后才能继续翻译");
    }

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

  private Map<String, String> ensureMetadata(Context context) {
    Map<String, String> metadata = context.getMetadata();
    if (metadata == null) {
      metadata = new HashMap<>();
      context.setMetadata(metadata);
    }
    return metadata;
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
