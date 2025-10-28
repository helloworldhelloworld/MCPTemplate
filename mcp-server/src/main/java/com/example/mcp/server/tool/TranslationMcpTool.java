package com.example.mcp.server.tool;

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

    @McpTool(name = "translation", description = "将输入文本翻译为目标语言")
    public TranslationResponse translate(TranslationRequest request) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("sourceText", request.getSourceText());
        variables.put("targetLocale", request.getTargetLocale());
        String promptTemplate = StringUtils.hasText(request.getPromptTemplate()) ? request.getPromptTemplate()
                : "将以下文本翻译为{{targetLocale}}: {{sourceText}}";

        PromptTemplate template = new PromptTemplate(promptTemplate, variables);
        String translated = chatClient.prompt(template).call().content();
        log.info("translation target={}", request.getTargetLocale());
        return new TranslationResponse(translated, "spring-ai");
    }
}
