package com.example.mcp.common;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class MessageFormatter {

    private final DateTimeFormatter formatter;

    public MessageFormatter(DateTimeFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
    }

    public String format(Message message) {
        Objects.requireNonNull(message, "message must not be null");
        return message.title() + " (" + formatter.format(message.createdAt()) + ")\n" + message.body();
    }
}
