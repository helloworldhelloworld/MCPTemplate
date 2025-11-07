package com.example.mcp.framework.async;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

import java.util.concurrent.CompletableFuture;

/**
 * 异步工具处理器接口
 */
@FunctionalInterface
public interface AsyncToolHandler<I, O> {

    /**
     * 异步处理请求
     *
     * @param context 上下文
     * @param input 输入载荷
     * @return 响应的CompletableFuture
     */
    CompletableFuture<StdResponse<O>> handleAsync(Context context, I input);
}
