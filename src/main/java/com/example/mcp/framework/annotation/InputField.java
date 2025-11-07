package com.example.mcp.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记工具的输入字段
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InputField {

    /**
     * 字段描述
     */
    String value();

    /**
     * 是否必需
     */
    boolean required() default true;
}
