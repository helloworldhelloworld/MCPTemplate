package com.example.mcp.server.config;

/**
 * Shared constants describing the Spring AI managed MCP HTTP endpoints.
 */
public final class McpServerPaths {

    public static final String BASE_PATH = "/mcp";
    public static final String SESSION_PATH = BASE_PATH + "/session";
    public static final String INVOKE_PATH = BASE_PATH + "/invoke";
    public static final String STREAM_PATH = BASE_PATH + "/stream";
    public static final String TOOLS_PATH = BASE_PATH + "/tools";
    public static final String GOVERNANCE_PATH = BASE_PATH + "/governance/audit";

    private McpServerPaths() {
    }
}
