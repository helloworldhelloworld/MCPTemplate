package com.example.mcp.framework.exception;

/**
 * MCP框架基础异常类
 */
public class McpException extends RuntimeException {

    private final String errorCode;

    public McpException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public McpException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "McpException{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
