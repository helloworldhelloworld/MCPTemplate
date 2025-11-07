package com.example.mcp.framework.client;

import com.example.mcp.common.Envelopes;

/**
 * 日志拦截器，记录客户端调用的详细信息
 */
public class LoggingInterceptor implements McpClientInterceptor {

    @Override
    public <I> void beforeInvoke(String toolName, Envelopes.RequestEnvelope<I> request) {
        System.out.println("[MCP-CLIENT] 调用工具: " + toolName +
                ", RequestId: " + request.getContext().getRequestId());
    }

    @Override
    public <I, O> void afterInvoke(String toolName, Envelopes.RequestEnvelope<I> request,
                                    Envelopes.ResponseEnvelope<O> response) {
        System.out.println("[MCP-CLIENT] 工具调用完成: " + toolName +
                ", 状态: " + response.getResponse().getStatus() +
                ", 耗时: " + response.getContext().getUsage().getLatencyMs() + "ms");
    }

    @Override
    public <I> void onError(String toolName, Envelopes.RequestEnvelope<I> request, Exception exception) {
        System.err.println("[MCP-CLIENT] 工具调用失败: " + toolName +
                ", 错误: " + exception.getMessage());
    }
}
