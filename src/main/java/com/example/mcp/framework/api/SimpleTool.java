package com.example.mcp.framework.api;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

import java.util.function.BiFunction;

/**
 * 简化的工具实现，使用函数式接口
 */
public class SimpleTool<I, O> implements ToolHandler<I, O> {

    private final BiFunction<Context, I, StdResponse<O>> function;

    public SimpleTool(BiFunction<Context, I, StdResponse<O>> function) {
        this.function = function;
    }

    @Override
    public StdResponse<O> handle(Context context, I input) {
        return function.apply(context, input);
    }

    /**
     * 创建一个简单的成功响应工具
     */
    public static <I, O> SimpleTool<I, O> success(BiFunction<Context, I, O> dataFunction) {
        return new SimpleTool<>((context, input) -> {
            try {
                O data = dataFunction.apply(context, input);
                return StdResponse.success("success", "处理成功", data);
            } catch (Exception e) {
                return StdResponse.error("processing_error", "处理失败: " + e.getMessage());
            }
        });
    }

    /**
     * 创建一个只处理输入不需要上下文的工具
     */
    public static <I, O> SimpleTool<I, O> of(java.util.function.Function<I, O> function) {
        return success((context, input) -> function.apply(input));
    }
}
