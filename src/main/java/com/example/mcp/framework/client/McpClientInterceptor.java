package com.example.mcp.framework.client;

import com.example.mcp.common.Envelopes;

/**
 * MCP客户端拦截器接口，用于在调用前后执行自定义逻辑
 */
public interface McpClientInterceptor {

    /**
     * 在工具调用之前执行
     *
     * @param toolName 工具名称
     * @param request 请求封装
     * @param <I> 请求载荷类型
     */
    <I> void beforeInvoke(String toolName, Envelopes.RequestEnvelope<I> request);

    /**
     * 在工具调用之后执行
     *
     * @param toolName 工具名称
     * @param request 请求封装
     * @param response 响应封装
     * @param <I> 请求载荷类型
     * @param <O> 响应载荷类型
     */
    <I, O> void afterInvoke(String toolName, Envelopes.RequestEnvelope<I> request,
                            Envelopes.ResponseEnvelope<O> response);

    /**
     * 在工具调用出现异常时执行
     *
     * @param toolName 工具名称
     * @param request 请求封装
     * @param exception 异常信息
     * @param <I> 请求载荷类型
     */
    <I> void onError(String toolName, Envelopes.RequestEnvelope<I> request, Exception exception);
}
