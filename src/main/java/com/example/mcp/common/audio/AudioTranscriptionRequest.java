package com.example.mcp.common.audio;

import java.util.Objects;

public final class AudioTranscriptionRequest {
    private final String audioSample;
    private final String locale;

    public AudioTranscriptionRequest(String audioSample, String locale) {
        this.audioSample = Objects.requireNonNull(audioSample, "audioSample must not be null");
        this.locale = Objects.requireNonNull(locale, "locale must not be null");
    }

    public String getAudioSample() {
        return audioSample;
    }

    public String getLocale() {
        return locale;
    }

    @Override
    public String toString() {
        return "AudioTranscriptionRequest{" +
                "audioSample='" + audioSample + '\'' +
                ", locale='" + locale + '\'' +
                '}';
    }
}
