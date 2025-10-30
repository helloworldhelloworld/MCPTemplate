package com.example.mcp.common.translation;

import java.util.Objects;

public final class TranslationRequest {
    private final String sourceText;
    private final String targetLocale;

    public TranslationRequest(String sourceText, String targetLocale) {
        this.sourceText = Objects.requireNonNull(sourceText, "sourceText must not be null");
        this.targetLocale = Objects.requireNonNull(targetLocale, "targetLocale must not be null");
    }

    public String getSourceText() {
        return sourceText;
    }

    public String getTargetLocale() {
        return targetLocale;
    }

    @Override
    public String toString() {
        return "TranslationRequest{" +
                "sourceText='" + sourceText + '\'' +
                ", targetLocale='" + targetLocale + '\'' +
                '}';
    }
}
