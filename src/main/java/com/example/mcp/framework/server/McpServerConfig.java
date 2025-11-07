package com.example.mcp.framework.server;

import java.time.Duration;
import java.util.Objects;

/**
 * MCP服务器配置类
 */
public class McpServerConfig {

    private String serverId = "mcp-server";
    private String serverVersion = "1.0.0";
    private boolean enableGovernance = true;
    private boolean enableAudit = true;
    private boolean enableMetrics = true;
    private Duration sessionTimeout = Duration.ofHours(1);
    private int maxConcurrentSessions = 1000;
    private boolean strictSchemaValidation = false;

    public McpServerConfig() {
    }

    public McpServerConfig(String serverId) {
        this.serverId = Objects.requireNonNull(serverId, "serverId must not be null");
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = Objects.requireNonNull(serverId, "serverId must not be null");
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = Objects.requireNonNull(serverVersion, "serverVersion must not be null");
    }

    public boolean isEnableGovernance() {
        return enableGovernance;
    }

    public void setEnableGovernance(boolean enableGovernance) {
        this.enableGovernance = enableGovernance;
    }

    public boolean isEnableAudit() {
        return enableAudit;
    }

    public void setEnableAudit(boolean enableAudit) {
        this.enableAudit = enableAudit;
    }

    public boolean isEnableMetrics() {
        return enableMetrics;
    }

    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }

    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Duration sessionTimeout) {
        this.sessionTimeout = Objects.requireNonNull(sessionTimeout, "sessionTimeout must not be null");
    }

    public int getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }

    public void setMaxConcurrentSessions(int maxConcurrentSessions) {
        if (maxConcurrentSessions <= 0) {
            throw new IllegalArgumentException("maxConcurrentSessions must be positive");
        }
        this.maxConcurrentSessions = maxConcurrentSessions;
    }

    public boolean isStrictSchemaValidation() {
        return strictSchemaValidation;
    }

    public void setStrictSchemaValidation(boolean strictSchemaValidation) {
        this.strictSchemaValidation = strictSchemaValidation;
    }

    @Override
    public String toString() {
        return "McpServerConfig{" +
                "serverId='" + serverId + '\'' +
                ", serverVersion='" + serverVersion + '\'' +
                ", enableGovernance=" + enableGovernance +
                ", enableAudit=" + enableAudit +
                ", enableMetrics=" + enableMetrics +
                ", sessionTimeout=" + sessionTimeout +
                ", maxConcurrentSessions=" + maxConcurrentSessions +
                ", strictSchemaValidation=" + strictSchemaValidation +
                '}';
    }
}
