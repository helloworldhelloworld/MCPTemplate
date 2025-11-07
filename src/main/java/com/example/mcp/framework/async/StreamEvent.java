package com.example.mcp.framework.async;

/**
 * 流式事件，用于表示处理过程中的中间状态
 */
public class StreamEvent {

    public enum Type {
        PROGRESS,      // 进度更新
        PARTIAL_RESULT, // 部分结果
        STATUS,        // 状态更新
        ERROR,         // 错误信息
        COMPLETE       // 完成标记
    }

    private final Type type;
    private final String message;
    private final Object data;
    private final double progress; // 0.0 到 1.0

    public StreamEvent(Type type, String message) {
        this(type, message, null, 0.0);
    }

    public StreamEvent(Type type, String message, Object data) {
        this(type, message, data, 0.0);
    }

    public StreamEvent(Type type, String message, Object data, double progress) {
        this.type = type;
        this.message = message;
        this.data = data;
        this.progress = Math.max(0.0, Math.min(1.0, progress));
    }

    public static StreamEvent progress(String message, double progress) {
        return new StreamEvent(Type.PROGRESS, message, null, progress);
    }

    public static StreamEvent partialResult(String message, Object data) {
        return new StreamEvent(Type.PARTIAL_RESULT, message, data);
    }

    public static StreamEvent status(String message) {
        return new StreamEvent(Type.STATUS, message);
    }

    public static StreamEvent error(String message) {
        return new StreamEvent(Type.ERROR, message);
    }

    public static StreamEvent complete() {
        return new StreamEvent(Type.COMPLETE, "处理完成");
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public double getProgress() {
        return progress;
    }

    @Override
    public String toString() {
        return "StreamEvent{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", progress=" + progress +
                '}';
    }
}
