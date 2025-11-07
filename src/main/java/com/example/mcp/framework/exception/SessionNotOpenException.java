package com.example.mcp.framework.exception;

/**
 * 会话未打开异常
 */
public class SessionNotOpenException extends McpException {

    public SessionNotOpenException() {
        super("session_not_open", "Session has not been opened. Please call openSession() first.");
    }

    public SessionNotOpenException(String message) {
        super("session_not_open", message);
    }
}
