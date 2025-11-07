package com.example.mcp.framework.exception;

/**
 * 工具执行异常
 */
public class ToolExecutionException extends McpException {

    private final String toolName;

    public ToolExecutionException(String toolName, String message) {
        super("tool_execution_error", message);
        this.toolName = toolName;
    }

    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super("tool_execution_error", message, cause);
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
}
