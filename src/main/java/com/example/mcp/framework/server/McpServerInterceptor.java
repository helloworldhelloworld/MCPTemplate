package com.example.mcp.framework.server;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes;
import com.example.mcp.common.StdResponse;

/**
 * MCP服务器拦截器接口，用于在工具处理前后执行自定义逻辑
 */
public interface McpServerInterceptor {

    /**
     * 在工具处理之前执行
     *
     * @param toolName 工具名称
     * @param context 上下文
     * @param payload 请求载荷
     * @param <I> 请求载荷类型
     * @return 是否继续执行，返回false将中断执行
     */
    <I> boolean beforeHandle(String toolName, Context context, I payload);

    /**
     * 在工具处理之后执行
     *
     * @param toolName 工具名称
     * @param context 上下文
     * @param payload 请求载荷
     * @param response 响应结果
     * @param <I> 请求载荷类型
     * @param <O> 响应载荷类型
     */
    <I, O> void afterHandle(String toolName, Context context, I payload, StdResponse<O> response);

    /**
     * 在工具处理出现异常时执行
     *
     * @param toolName 工具名称
     * @param context 上下文
     * @param payload 请求载荷
     * @param exception 异常信息
     * @param <I> 请求载荷类型
     */
    <I> void onError(String toolName, Context context, I payload, Exception exception);
}
