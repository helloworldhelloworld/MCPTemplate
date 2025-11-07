package com.example.mcp.framework.client;

import java.time.Duration;
import java.util.Objects;

/**
 * MCP客户端配置类，用于配置客户端行为
 */
public class McpClientConfig {

    private String clientId = "mcp-client";
    private String defaultLocale = "zh-CN";
    private Duration requestTimeout = Duration.ofSeconds(30);
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofMillis(500);
    private boolean enableMetrics = true;
    private boolean enableLogging = true;

    public McpClientConfig() {
    }

    public McpClientConfig(String clientId, String defaultLocale) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale must not be null");
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = Objects.requireNonNull(defaultLocale, "defaultLocale must not be null");
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay must not be null");
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    @Override
    public String toString() {
        return "McpClientConfig{" +
                "clientId='" + clientId + '\'' +
                ", defaultLocale='" + defaultLocale + '\'' +
                ", requestTimeout=" + requestTimeout +
                ", maxRetries=" + maxRetries +
                ", retryDelay=" + retryDelay +
                ", enableMetrics=" + enableMetrics +
                ", enableLogging=" + enableLogging +
                '}';
    }
}
