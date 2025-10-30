package com.example.mcp.common.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal session handshake payload. The server uses the information to personalise the
 * shared {@code Context} that is returned to the client.
 */
public final class SessionOpenRequest {
    private String clientId;
    private String locale = "zh-CN";
    private final Map<String, String> metadata = new HashMap<>();

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
