package com.example.mcp.framework.springai;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.framework.api.ToolHandler;

public abstract class SpringAiTool<I, O> implements ToolHandler<I, O> {

    private final SpringAiService springAiService;

    protected SpringAiTool(SpringAiService springAiService) {
        this.springAiService = springAiService;
    }

    protected SpringAiService getSpringAiService() {
        return springAiService;
    }

    protected abstract String systemPrompt(Context context, I input);

    protected abstract String userPrompt(Context context, I input);

    protected abstract StdResponse<O> mapToResponse(String modelResponse, Context context, I input);

    @Override
    public StdResponse<O> handle(Context context, I input) {
        String result = springAiService.chat(systemPrompt(context, input), userPrompt(context, input));
        return mapToResponse(result, context, input);
    }
}
