package com.example.mcp.framework.server;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流拦截器，用于控制客户端调用频率
 */
public class RateLimitInterceptor implements McpServerInterceptor {

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final int maxRequestsPerMinute;

    public RateLimitInterceptor(int maxRequestsPerMinute) {
        if (maxRequestsPerMinute <= 0) {
            throw new IllegalArgumentException("maxRequestsPerMinute must be positive");
        }
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    public <I> boolean beforeHandle(String toolName, Context context, I payload) {
        String clientId = context.getClientId();
        RateLimiter limiter = limiters.computeIfAbsent(clientId, k -> new RateLimiter(maxRequestsPerMinute));

        if (!limiter.allowRequest()) {
            System.err.println("[MCP-SERVER] 限流拒绝: 客户端 " + clientId + " 超过请求频率限制");
            return false;
        }

        return true;
    }

    @Override
    public <I, O> void afterHandle(String toolName, Context context, I payload, StdResponse<O> response) {
    }

    @Override
    public <I> void onError(String toolName, Context context, I payload, Exception exception) {
    }

    private static class RateLimiter {
        private final int maxRequests;
        private final AtomicInteger counter = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        public RateLimiter(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            long elapsed = now - windowStart;

            // 如果超过1分钟，重置计数器
            if (elapsed >= 60000) {
                counter.set(0);
                windowStart = now;
            }

            int current = counter.incrementAndGet();
            return current <= maxRequests;
        }
    }
}
