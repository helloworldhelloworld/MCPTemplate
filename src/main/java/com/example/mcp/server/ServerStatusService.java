package com.example.mcp.server;

import com.example.mcp.common.Message;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.InvocationAuditRecord;
import java.time.Instant;
import java.util.Objects;

public final class ServerStatusService {

    private final String serviceName;
    private final McpServer server;

    public ServerStatusService(String serviceName, McpServer server) {
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName must not be null");
        this.server = Objects.requireNonNull(server, "server must not be null");
    }

    public Message statusMessage() {
        int toolCount = server.listTools().size();
        String body = "服务 '" + serviceName + "' 已启动，当前已注册 " + toolCount + " 个工具能力。";
        return new Message("Server Status", body, Instant.now());
    }

    public Message governanceSnapshot() {
        GovernanceReport report = server.governanceReport();
        StringBuilder builder = new StringBuilder();
        builder.append("最近调用记录：\n");
        if (report.getRecords().isEmpty()) {
            builder.append("尚无调用，等待客户端触发。");
        } else {
            for (InvocationAuditRecord record : report.getRecords()) {
                builder.append("- ")
                        .append(record.getTool())
                        .append(" -> ")
                        .append(record.getStatus())
                        .append(" (latency=")
                        .append(record.getLatencyMs())
                        .append("ms)\n");
            }
        }
        return new Message("Governance", builder.toString(), Instant.now());
    }
}
