package com.example.mcp.common;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Captures the shared execution metadata exchanged between the client and server.
 * The object intentionally keeps a mutable shape so the demo client can reuse the
 * same context instance across multiple tool invocations.
 */
public final class Context {

    private String sessionId;
    private String clientId;
    private String requestId;
    private String locale;
    private Instant createdAt = Instant.now();
    private final Map<String, String> metadata = new HashMap<>();
    private Usage usage = new Usage();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void putMetadata(String key, String value) {
        metadata.put(key, value);
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = Objects.requireNonNull(usage, "usage must not be null");
    }

    public Context copy() {
        Context context = new Context();
        context.sessionId = this.sessionId;
        context.clientId = this.clientId;
        context.requestId = this.requestId;
        context.locale = this.locale;
        context.createdAt = this.createdAt;
        context.metadata.putAll(this.metadata);
        context.usage = this.usage.copy();
        return context;
    }

    public static final class Usage {
        private int inputTokens;
        private int outputTokens;
        private long latencyMs;

        public int getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(int inputTokens) {
            this.inputTokens = inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(int outputTokens) {
            this.outputTokens = outputTokens;
        }

        public long getLatencyMs() {
            return latencyMs;
        }

        public void setLatencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
        }

        public Usage copy() {
            Usage usage = new Usage();
            usage.inputTokens = this.inputTokens;
            usage.outputTokens = this.outputTokens;
            usage.latencyMs = this.latencyMs;
            return usage;
        }

        @Override
        public String toString() {
            return "Usage{" +
                    "inputTokens=" + inputTokens +
                    ", outputTokens=" + outputTokens +
                    ", latencyMs=" + latencyMs +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Context{" +
                "sessionId='" + sessionId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", locale='" + locale + '\'' +
                ", createdAt=" + createdAt +
                ", metadata=" + Collections.unmodifiableMap(metadata) +
                ", usage=" + usage +
                '}';
    }
}
