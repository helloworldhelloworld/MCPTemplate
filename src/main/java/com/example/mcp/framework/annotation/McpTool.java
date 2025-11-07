package com.example.mcp.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为MCP工具
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {

    /**
     * 工具名称（唯一标识符）
     */
    String name();

    /**
     * 工具显示名称
     */
    String displayName() default "";

    /**
     * 工具描述
     */
    String description() default "";
}
