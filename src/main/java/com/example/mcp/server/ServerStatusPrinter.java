package com.example.mcp.server;

import com.example.mcp.common.Message;
import com.example.mcp.common.MessageFormatter;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class ServerStatusPrinter {

    private final ServerStatusService statusService;
    private final MessageFormatter formatter;

    public ServerStatusPrinter(ServerStatusService statusService) {
        this.statusService = Objects.requireNonNull(statusService, "statusService must not be null");
        this.formatter = new MessageFormatter(DateTimeFormatter.ISO_INSTANT);
    }

    public String asText() {
        Message status = statusService.statusMessage();
        Message governance = statusService.governanceSnapshot();
        return formatter.format(status) + "\n\n" + formatter.format(governance);
    }
}
