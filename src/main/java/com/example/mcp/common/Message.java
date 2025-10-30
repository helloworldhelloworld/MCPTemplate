package com.example.mcp.common;

import java.time.Instant;
import java.util.Objects;

public final class Message {

    private final String title;
    private final String body;
    private final Instant createdAt;

    public Message(String title, String body, Instant createdAt) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.body = Objects.requireNonNull(body, "body must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message message)) {
            return false;
        }
        return title.equals(message.title) && body.equals(message.body) && createdAt.equals(message.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, body, createdAt);
    }

    @Override
    public String toString() {
        return "Message{" +
                "title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
