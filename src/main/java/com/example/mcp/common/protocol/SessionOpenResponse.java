package com.example.mcp.common.protocol;

import com.example.mcp.common.Context;

/**
 * Response returned by the server after successfully creating a session.
 */
public final class SessionOpenResponse {
    private final String sessionId;
    private final Context context;
    private final String greeting;

    public SessionOpenResponse(String sessionId, Context context, String greeting) {
        this.sessionId = sessionId;
        this.context = context;
        this.greeting = greeting;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Context getContext() {
        return context;
    }

    public String getGreeting() {
        return greeting;
    }

    @Override
    public String toString() {
        return "SessionOpenResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", context=" + context +
                ", greeting='" + greeting + '\'' +
                '}';
    }
}
