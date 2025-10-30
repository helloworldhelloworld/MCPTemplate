package com.example.mcp.common.qa;

import java.util.Objects;

public final class QaResponse {
    private final String answer;
    private final double confidence;

    public QaResponse(String answer, double confidence) {
        this.answer = Objects.requireNonNull(answer, "answer must not be null");
        this.confidence = confidence;
    }

    public String getAnswer() {
        return answer;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "QaResponse{" +
                "answer='" + answer + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
