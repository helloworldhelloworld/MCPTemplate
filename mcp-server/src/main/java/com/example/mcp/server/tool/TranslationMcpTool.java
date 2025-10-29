package com.example.mcp.server.tool;

import com.example.mcp.common.StdResponse;
import com.example.mcp.common.translation.TranslationRequest;
import com.example.mcp.common.translation.TranslationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用 Spring AI MCP Server 注解声明的翻译工具，演示如何沿标准协议暴露能力。
 */
@Component
public class TranslationMcpTool {

    private static final Logger log = LoggerFactory.getLogger(TranslationMcpTool.class);

    private final ChatClient chatClient;

    public TranslationMcpTool(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @McpTool(
            name = "mcp.translation.invoke",
            description = "将输入文本翻译为目标语言",
            inputSchema = "schema/translation-request.json",
            outputSchema = "schema/translation-response.json")
    public StdResponse<TranslationResponse> translate(TranslationRequest request) {
        if (request == null || !StringUtils.hasText(request.getSourceText())) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("missingField", "sourceText");
            payload.put("prompt", "请补充需要翻译的原文内容");
            payload.put("examples", "例如：你好，世界");
            return StdResponse.clarify("TRANSLATION_TEXT_REQUIRED", "需要提供原文后才能继续翻译", payload);
        }

        if (!StringUtils.hasText(request.getTargetLocale())) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("missingField", "targetLocale");
            payload.put("prompt", "请选择目标语言，例如 zh-CN、en-US");
            payload.put("options", new String[] {"zh-CN", "en-US", "ja-JP"});
            return StdResponse.clarify("TRANSLATION_TARGET_REQUIRED", "需要确认目标语言后才能继续翻译", payload);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("sourceText", request.getSourceText());
        variables.put("targetLocale", request.getTargetLocale());
        String promptTemplate = StringUtils.hasText(request.getPromptTemplate()) ? request.getPromptTemplate()
                : "将以下文本翻译为{{targetLocale}}: {{sourceText}}";

        PromptTemplate template = new PromptTemplate(promptTemplate, variables);
        String translated = translateWithModel(template);
        log.info("translation target={}", request.getTargetLocale());

        TranslationResponse response = new TranslationResponse();
        response.setSourceText(request.getSourceText());
        response.setTargetLocale(request.getTargetLocale());
        response.setTranslatedText(translated);
        response.setModel("spring-ai");
        return StdResponse.success("TRANSLATED", "Translation completed", response);
    }

    /**
     * Hook for tests to override the actual model invocation while production code
     * keeps delegating to {@link ChatClient}.
     */
    protected String translateWithModel(PromptTemplate template) {
        return chatClient.prompt(template).call().content();
    }
}
