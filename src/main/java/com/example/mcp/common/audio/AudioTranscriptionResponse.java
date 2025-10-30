package com.example.mcp.common.audio;

import java.util.Objects;

public final class AudioTranscriptionResponse {
    private final String transcript;
    private final double confidence;

    public AudioTranscriptionResponse(String transcript, double confidence) {
        this.transcript = Objects.requireNonNull(transcript, "transcript must not be null");
        this.confidence = confidence;
    }

    public String getTranscript() {
        return transcript;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "AudioTranscriptionResponse{" +
                "transcript='" + transcript + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
