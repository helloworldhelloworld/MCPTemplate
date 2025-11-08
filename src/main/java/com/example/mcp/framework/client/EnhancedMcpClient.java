package com.example.mcp.framework.client;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import com.example.mcp.framework.api.McpClient;
import com.example.mcp.framework.api.McpServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 增强的MCP客户端实现，支持配置、拦截器、重试等功能
 */
public class EnhancedMcpClient implements McpClient {

    private final McpServer server;
    private final McpClientConfig config;
    private final List<McpClientInterceptor> interceptors = new ArrayList<>();
    private Context sessionContext;

    public EnhancedMcpClient(McpServer server) {
        this(server, new McpClientConfig());
    }

    public EnhancedMcpClient(McpServer server, McpClientConfig config) {
        this.server = Objects.requireNonNull(server, "server must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");

        // 默认添加日志和指标拦截器
        if (config.isEnableLogging()) {
            addInterceptor(new LoggingInterceptor());
        }
        if (config.isEnableMetrics()) {
            addInterceptor(new MetricsInterceptor());
        }
    }

    /**
     * 添加拦截器
     */
    public void addInterceptor(McpClientInterceptor interceptor) {
        interceptors.add(Objects.requireNonNull(interceptor, "interceptor must not be null"));
    }

    /**
     * 获取配置
     */
    public McpClientConfig getConfig() {
        return config;
    }

    @Override
    public SessionOpenResponse openSession(String locale) {
        SessionOpenRequest request = new SessionOpenRequest();
        request.setClientId(config.getClientId());
        request.setLocale(locale != null ? locale : config.getDefaultLocale());
        request.getMetadata().put("client-version", "1.0.0");
        request.getMetadata().put("timestamp", String.valueOf(System.currentTimeMillis()));

        SessionOpenResponse response = server.openSession(request);
        this.sessionContext = response.getContext().copy();

        if (config.isEnableLogging()) {
            System.out.println("[MCP-CLIENT] 会话已建立: SessionId=" + sessionContext.getSessionId());
        }

        return response;
    }

    @Override
    public List<ToolDescriptor> listTools() {
        ensureSessionOpen();
        return server.listTools();
    }

    @Override
    public Optional<ToolDescriptor> describeTool(String name) {
        ensureSessionOpen();
        return server.describeTool(name);
    }

    @Override
    public <I, O> Envelopes.ResponseEnvelope<O> invoke(String toolName, I payload, Class<O> responseType) {
        ensureSessionOpen();

        Context context = sessionContext.copy();
        context.setRequestId(UUID.randomUUID().toString());
        Envelopes.RequestEnvelope<I> request = new Envelopes.RequestEnvelope<>(toolName, context, payload);

        // 执行前置拦截器
        for (McpClientInterceptor interceptor : interceptors) {
            try {
                interceptor.beforeInvoke(toolName, request);
            } catch (Exception e) {
                if (config.isEnableLogging()) {
                    System.err.println("[MCP-CLIENT] 拦截器前置处理失败: " + e.getMessage());
                }
            }
        }

        Envelopes.ResponseEnvelope<O> response = null;
        Exception lastException = null;

        // 重试逻辑
        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
            try {
                response = server.invoke(request, responseType);

                // 更新会话上下文
                this.sessionContext = response.getContext().copy();

                // 执行后置拦截器
                for (McpClientInterceptor interceptor : interceptors) {
                    try {
                        interceptor.afterInvoke(toolName, request, response);
                    } catch (Exception e) {
                        if (config.isEnableLogging()) {
                            System.err.println("[MCP-CLIENT] 拦截器后置处理失败: " + e.getMessage());
                        }
                    }
                }

                return response;
            } catch (Exception e) {
                lastException = e;

                if (attempt < config.getMaxRetries()) {
                    if (config.isEnableLogging()) {
                        System.err.println("[MCP-CLIENT] 调用失败，正在重试 (" + (attempt + 1) + "/" + config.getMaxRetries() + "): " + e.getMessage());
                    }

                    try {
                        Thread.sleep(config.getRetryDelay().toMillis() * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // 执行错误拦截器
                    for (McpClientInterceptor interceptor : interceptors) {
                        try {
                            interceptor.onError(toolName, request, e);
                        } catch (Exception ie) {
                            if (config.isEnableLogging()) {
                                System.err.println("[MCP-CLIENT] 拦截器错误处理失败: " + ie.getMessage());
                            }
                        }
                    }
                }
            }
        }

        // 所有重试都失败，返回错误响应
        StdResponse<O> errorResponse = StdResponse.error("client_error",
                "调用失败: " + (lastException != null ? lastException.getMessage() : "未知错误"));
        return new Envelopes.ResponseEnvelope<>(toolName, context, errorResponse, null);
    }

    @Override
    public Context getSessionContext() {
        return sessionContext;
    }

    /**
     * 确保会话已经打开
     */
    private void ensureSessionOpen() {
        if (sessionContext == null) {
            throw new IllegalStateException("会话未打开，请先调用 openSession()");
        }
    }

    /**
     * 关闭会话
     */
    public void closeSession() {
        if (sessionContext != null) {
            if (config.isEnableLogging()) {
                System.out.println("[MCP-CLIENT] 会话已关闭: SessionId=" + sessionContext.getSessionId());
            }
            sessionContext = null;
        }
    }
}
