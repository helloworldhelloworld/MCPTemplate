package com.example.mcp.common;

import java.util.Objects;

/**
 * Canonical response wrapper shared by the client and server. The implementation keeps
 * the structure lightweight so it can be serialised or logged without extra dependencies.
 */
public final class StdResponse<T> {

    public enum Status {
        SUCCESS,
        ERROR,
        CLARIFICATION
    }

    private final Status status;
    private final String code;
    private final String message;
    private final T data;

    private StdResponse(Status status, String code, String message, T data) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> StdResponse<T> success(String code, String message, T data) {
        return new StdResponse<>(Status.SUCCESS, code, message, data);
    }

    public static <T> StdResponse<T> error(String code, String message) {
        return new StdResponse<>(Status.ERROR, code, message, null);
    }

    public static <T> StdResponse<T> clarification(String code, String message, T data) {
        return new StdResponse<>(Status.CLARIFICATION, code, message, data);
    }

    public Status getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    @Override
    public String toString() {
        return "StdResponse{" +
                "status=" + status +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
