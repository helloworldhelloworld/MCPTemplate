package com.example.mcp.framework.client;

import com.example.mcp.common.Envelopes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标拦截器，收集客户端调用的统计信息
 */
public class MetricsInterceptor implements McpClientInterceptor {

    private final ConcurrentHashMap<String, ToolMetrics> metricsMap = new ConcurrentHashMap<>();

    @Override
    public <I> void beforeInvoke(String toolName, Envelopes.RequestEnvelope<I> request) {
        ToolMetrics metrics = metricsMap.computeIfAbsent(toolName, k -> new ToolMetrics());
        metrics.incrementTotal();
    }

    @Override
    public <I, O> void afterInvoke(String toolName, Envelopes.RequestEnvelope<I> request,
                                    Envelopes.ResponseEnvelope<O> response) {
        ToolMetrics metrics = metricsMap.get(toolName);
        if (metrics != null) {
            if ("SUCCESS".equals(response.getResponse().getStatus())) {
                metrics.incrementSuccess();
            } else {
                metrics.incrementFailure();
            }
            metrics.addLatency(response.getContext().getUsage().getLatencyMs());
        }
    }

    @Override
    public <I> void onError(String toolName, Envelopes.RequestEnvelope<I> request, Exception exception) {
        ToolMetrics metrics = metricsMap.get(toolName);
        if (metrics != null) {
            metrics.incrementFailure();
        }
    }

    public ToolMetrics getMetrics(String toolName) {
        return metricsMap.get(toolName);
    }

    public ConcurrentHashMap<String, ToolMetrics> getAllMetrics() {
        return metricsMap;
    }

    public static class ToolMetrics {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successCalls = new AtomicLong(0);
        private final AtomicLong failureCalls = new AtomicLong(0);
        private final AtomicLong totalLatency = new AtomicLong(0);

        public void incrementTotal() {
            totalCalls.incrementAndGet();
        }

        public void incrementSuccess() {
            successCalls.incrementAndGet();
        }

        public void incrementFailure() {
            failureCalls.incrementAndGet();
        }

        public void addLatency(long latency) {
            totalLatency.addAndGet(latency);
        }

        public long getTotalCalls() {
            return totalCalls.get();
        }

        public long getSuccessCalls() {
            return successCalls.get();
        }

        public long getFailureCalls() {
            return failureCalls.get();
        }

        public double getAverageLatency() {
            long total = totalCalls.get();
            return total > 0 ? (double) totalLatency.get() / total : 0;
        }

        @Override
        public String toString() {
            return "ToolMetrics{" +
                    "total=" + totalCalls.get() +
                    ", success=" + successCalls.get() +
                    ", failure=" + failureCalls.get() +
                    ", avgLatency=" + String.format("%.2f", getAverageLatency()) + "ms" +
                    '}';
        }
    }
}
