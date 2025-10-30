package com.example.mcp.common.qa;

import java.util.Objects;

public final class QaRequest {
    private final String question;

    public QaRequest(String question) {
        this.question = Objects.requireNonNull(question, "question must not be null");
    }

    public String getQuestion() {
        return question;
    }

    @Override
    public String toString() {
        return "QaRequest{" +
                "question='" + question + '\'' +
                '}';
    }
}
