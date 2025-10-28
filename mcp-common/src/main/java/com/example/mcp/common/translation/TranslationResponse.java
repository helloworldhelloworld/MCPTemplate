package com.example.mcp.common.translation;

/**
 * 翻译工具的响应载荷，映射 MCP Envelope 中的数据部分。
 */
public class TranslationResponse {

    private String sourceText;
    private String targetLocale;
    private String translatedText;
    private String model;

    public TranslationResponse() {
    }

    public TranslationResponse(String sourceText, String targetLocale, String translatedText, String model) {
        this.sourceText = sourceText;
        this.targetLocale = targetLocale;
        this.translatedText = translatedText;
        this.model = model;
    }

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

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
