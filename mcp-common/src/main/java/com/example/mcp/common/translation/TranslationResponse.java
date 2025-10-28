package com.example.mcp.common.translation;

/**
 * 翻译工具的响应载荷，映射 MCP Envelope 中的数据部分。
 */
public class TranslationResponse {

    private final String translatedText;
    private final String model;

    public TranslationResponse(String translatedText, String model) {
        this.translatedText = translatedText;
        this.model = model;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getModel() {
        return model;
    }
}
