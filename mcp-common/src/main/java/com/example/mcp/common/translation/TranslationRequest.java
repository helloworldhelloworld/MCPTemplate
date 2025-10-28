package com.example.mcp.common.translation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 翻译工具的输入负载，体现 MCP 协议 L3 层的 payload 结构约束。
 */
public class TranslationRequest {

    @NotBlank
    private String sourceText;

    @NotBlank
    @Pattern(regexp = "[a-z]{2}-[A-Z]{2}")
    private String targetLocale;

    private String promptTemplate;

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getTargetLocale() {
        return targetLocale;
    }

    public void setTargetLocale(String targetLocale) {
        this.targetLocale = targetLocale;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }
}
