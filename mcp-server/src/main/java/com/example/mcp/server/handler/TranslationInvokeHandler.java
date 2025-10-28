package com.example.mcp.server.handler;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.example.mcp.common.translation.TranslationRequest;
import com.example.mcp.common.translation.TranslationResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TranslationInvokeHandler
    implements ToolHandler<TranslationRequest, TranslationResponse> {

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

    if (request == null || !StringUtils.hasText(request.getSourceText())) {
      metadata.put("clarify.missingField", "sourceText");
      metadata.put("clarify.prompt", "请补充需要翻译的原文内容");
      metadata.put("clarify.examples", "例如：你好，世界");
      return StdResponse.clarify(
          "TRANSLATION_TEXT_REQUIRED", "需要提供原文后才能继续翻译");
    }

    if (!StringUtils.hasText(request.getTargetLocale())) {
      metadata.put("clarify.missingField", "targetLocale");
      metadata.put("clarify.prompt", "请选择目标语言，例如 zh-CN、en-US");
      metadata.put("clarify.options", "zh-CN,en-US,ja-JP");
      return StdResponse.clarify(
          "TRANSLATION_TARGET_REQUIRED", "需要确认目标语言后才能继续翻译");
    }

    TranslationResponse response = new TranslationResponse();
    response.setSourceText(request.getSourceText());
    response.setTargetLocale(request.getTargetLocale());
    response.setTranslatedText(request.getSourceText().toUpperCase());
    response.setModel("mock-transformer");

    Context.Usage usage = new Context.Usage();
    usage.setLatencyMs(12L);
    context.setUsage(usage);
    return StdResponse.success("TRANSLATED", "Translation completed", response);
  }

  @Override
  public ToolDescriptor describe() {
    ToolDescriptor descriptor = new ToolDescriptor();
    descriptor.setName(getToolName());
    descriptor.setTitle("翻译工具");
    descriptor.setDescription("根据 MCP 协议演示翻译调用全流程");
    descriptor.setInputSchema("schema/translation-request.json");
    descriptor.setOutputSchema("schema/translation-response.json");
    descriptor.addCapability("invocation");
    descriptor.addCapability("clarification");
    return descriptor;
  }

  private Map<String, String> ensureMetadata(Context context) {
    Map<String, String> metadata = context.getMetadata();
    if (metadata == null) {
      metadata = new HashMap<>();
      context.setMetadata(metadata);
    }
    return metadata;
  }
}
