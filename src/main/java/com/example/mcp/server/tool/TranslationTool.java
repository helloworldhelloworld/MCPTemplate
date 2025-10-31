package com.example.mcp.server.tool;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.translation.TranslationRequest;
import com.example.mcp.common.translation.TranslationResponse;
import com.example.mcp.framework.springai.SpringAiTool;
import com.example.mcp.framework.springai.SpringAiService;
import org.springframework.ai.tool.annotation.Tool;

public final class TranslationTool extends SpringAiTool<TranslationRequest, TranslationResponse> {

    public TranslationTool(SpringAiService springAiService) {
        super(springAiService);
    }

    @Override
    protected String systemPrompt(Context context, TranslationRequest input) {
        return "You are a translation engine targeting locale " + input.getTargetLocale();
    }

    @Override
    protected String userPrompt(Context context, TranslationRequest input) {
        return input.getSourceText();
    }

    @Override
    protected StdResponse<TranslationResponse> mapToResponse(String modelResponse, Context context, TranslationRequest input) {
        TranslationResponse translationResponse = new TranslationResponse(modelResponse, input.getTargetLocale());
        return StdResponse.success("translation", "翻译完成", translationResponse);
    }

    @Override
    @Tool(name = "translation", title = "文本翻译", description = "基于 Spring AI 服务的示例翻译工具")
    public StdResponse<TranslationResponse> handle(Context context, TranslationRequest input) {
        return super.handle(context, input);
    }
}
