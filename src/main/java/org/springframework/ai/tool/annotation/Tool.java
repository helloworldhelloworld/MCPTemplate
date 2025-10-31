package org.springframework.ai.tool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Minimal representation of Spring AI's {@code @Tool} annotation so that the
 * example code can be compiled without bringing the full Spring AI dependency.
 * <p>
 * The attributes mirror the canonical annotation, allowing downstream code to
 * attach metadata such as the tool name and description directly on handler
 * methods. This keeps the framework extensible while showcasing how Spring AI
 * applications typically describe tools for automatic registration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {

    /**
     * The unique identifier of the tool exposed to the MCP client.
     */
    String name();

    /**
     * Human readable summary of the tool's intent.
     */
    String description() default "";

    /**
     * Optional display name shown in human facing catalogs.
     */
    String title() default "";
}
