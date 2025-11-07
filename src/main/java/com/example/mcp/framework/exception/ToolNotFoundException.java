package com.example.mcp.framework.exception;

/**
 * 工具未找到异常
 */
public class ToolNotFoundException extends McpException {

    private final String toolName;

    public ToolNotFoundException(String toolName) {
        super("tool_not_found", "Tool not found: " + toolName);
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
}
