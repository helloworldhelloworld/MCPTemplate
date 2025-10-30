package com.example.mcp.common;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Request/response envelopes abstract the transport concerns so that the demo can emulate
 * the Model Context Protocol interaction flow without relying on an actual wire protocol.
 */
public final class Envelopes {

    private Envelopes() {
    }

    public static final class RequestEnvelope<T> {
        private final String tool;
        private final Context context;
        private final T payload;
        private final Map<String, String> attachments = new HashMap<>();

        public RequestEnvelope(String tool, Context context, T payload) {
            this.tool = Objects.requireNonNull(tool, "tool must not be null");
            this.context = Objects.requireNonNull(context, "context must not be null");
            this.payload = Objects.requireNonNull(payload, "payload must not be null");
        }

        public String getTool() {
            return tool;
        }

        public Context getContext() {
            return context;
        }

        public T getPayload() {
            return payload;
        }

        public Map<String, String> getAttachments() {
            return attachments;
        }

        @Override
        public String toString() {
            return "RequestEnvelope{" +
                    "tool='" + tool + '\'' +
                    ", context=" + context +
                    ", payload=" + payload +
                    ", attachments=" + attachments +
                    '}';
        }
    }

    public static final class ResponseEnvelope<T> {
        private final String tool;
        private final Context context;
        private final StdResponse<T> response;
        private final UiCard uiCard;

        public ResponseEnvelope(String tool, Context context, StdResponse<T> response, UiCard uiCard) {
            this.tool = Objects.requireNonNull(tool, "tool must not be null");
            this.context = Objects.requireNonNull(context, "context must not be null");
            this.response = Objects.requireNonNull(response, "response must not be null");
            this.uiCard = uiCard;
        }

        public String getTool() {
            return tool;
        }

        public Context getContext() {
            return context;
        }

        public StdResponse<T> getResponse() {
            return response;
        }

        public UiCard getUiCard() {
            return uiCard;
        }

        @Override
        public String toString() {
            return "ResponseEnvelope{" +
                    "tool='" + tool + '\'' +
                    ", context=" + context +
                    ", response=" + response +
                    ", uiCard=" + uiCard +
                    '}';
        }
    }

    public static final class StreamEventEnvelope<T> {
        private final String tool;
        private final String event;
        private final Instant emittedAt;
        private final T data;

        public StreamEventEnvelope(String tool, String event, Instant emittedAt, T data) {
            this.tool = Objects.requireNonNull(tool, "tool must not be null");
            this.event = Objects.requireNonNull(event, "event must not be null");
            this.emittedAt = Objects.requireNonNull(emittedAt, "emittedAt must not be null");
            this.data = data;
        }

        public String getTool() {
            return tool;
        }

        public String getEvent() {
            return event;
        }

        public Instant getEmittedAt() {
            return emittedAt;
        }

        public T getData() {
            return data;
        }

        @Override
        public String toString() {
            return "StreamEventEnvelope{" +
                    "tool='" + tool + '\'' +
                    ", event='" + event + '\'' +
                    ", emittedAt=" + emittedAt +
                    ", data=" + data +
                    '}';
        }
    }

    public static final class UiCard {
        private String title;
        private String body;
        private final Map<String, String> actions = new HashMap<>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public Map<String, String> getActions() {
            return actions;
        }

        @Override
        public String toString() {
            return "UiCard{" +
                    "title='" + title + '\'' +
                    ", body='" + body + '\'' +
                    ", actions=" + actions +
                    '}';
        }
    }
}
