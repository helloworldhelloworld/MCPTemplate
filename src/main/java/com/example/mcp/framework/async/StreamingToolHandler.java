package com.example.mcp.framework.async;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

import java.util.function.Consumer;

/**
 * 流式工具处理器接口，支持推送中间事件
 */
@FunctionalInterface
public interface StreamingToolHandler<I, O> {

    /**
     * 处理请求并推送流式事件
     *
     * @param context 上下文
     * @param input 输入载荷
     * @param eventConsumer 事件消费者
     * @return 最终响应
     */
    StdResponse<O> handleWithStream(Context context, I input, Consumer<StreamEvent> eventConsumer);
}
