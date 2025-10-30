package com.example.mcp.common.protocol;

import com.example.mcp.common.StdResponse;
import java.time.Instant;
import java.util.Objects;

/**
 * Captures the outcome of a tool invocation for governance reporting.
 */
public final class InvocationAuditRecord {
    private final String requestId;
    private final String tool;
    private final StdResponse.Status status;
    private final long latencyMs;
    private final Instant timestamp;

    public InvocationAuditRecord(String requestId, String tool, StdResponse.Status status,
                                 long latencyMs, Instant timestamp) {
        this.requestId = Objects.requireNonNull(requestId, "requestId must not be null");
        this.tool = Objects.requireNonNull(tool, "tool must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.latencyMs = latencyMs;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTool() {
        return tool;
    }

    public StdResponse.Status getStatus() {
        return status;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "InvocationAuditRecord{" +
                "requestId='" + requestId + '\'' +
                ", tool='" + tool + '\'' +
                ", status=" + status +
                ", latencyMs=" + latencyMs +
                ", timestamp=" + timestamp +
                '}';
    }
}
