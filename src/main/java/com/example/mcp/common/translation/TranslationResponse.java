package com.example.mcp.common.translation;

import java.util.Objects;

public final class TranslationResponse {
    private final String translatedText;
    private final String detectedSourceLocale;

    public TranslationResponse(String translatedText, String detectedSourceLocale) {
        this.translatedText = Objects.requireNonNull(translatedText, "translatedText must not be null");
        this.detectedSourceLocale = Objects.requireNonNull(detectedSourceLocale, "detectedSourceLocale must not be null");
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public String getDetectedSourceLocale() {
        return detectedSourceLocale;
    }

    @Override
    public String toString() {
        return "TranslationResponse{" +
                "translatedText='" + translatedText + '\'' +
                ", detectedSourceLocale='" + detectedSourceLocale + '\'' +
                '}';
    }
}
