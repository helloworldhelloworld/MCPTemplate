package com.example.mcp.server.config;

import org.springframework.ai.mcp.server.annotation.McpServer;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the Spring AI MCP server auto-configuration so tools declared with
 * {@code @McpTool} are exported via the framework provided endpoints instead of
 * bespoke controllers.
 */
@Configuration
@McpServer
public class SpringAiMcpServerConfig {
}
