package com.example.mcp.framework.server;

/**
 * MCP服务器配置类
 */
public class McpServerConfig {

    private boolean enableAudit = true;

    public McpServerConfig() {
    }

    public boolean isEnableAudit() {
        return enableAudit;
    }

    public void setEnableAudit(boolean enableAudit) {
        this.enableAudit = enableAudit;
    }

    @Override
    public String toString() {
        return "McpServerConfig{" +
                "enableAudit=" + enableAudit +
                '}';
    }
}
