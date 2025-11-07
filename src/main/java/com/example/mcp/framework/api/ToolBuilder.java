package com.example.mcp.framework.api;

import com.example.mcp.common.protocol.ToolDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 工具构建器，提供链式API构建工具注册
 */
public class ToolBuilder<I, O> {

    private String name;
    private String displayName;
    private String description;
    private final Map<String, String> inputFields = new LinkedHashMap<>();
    private final Map<String, String> outputFields = new LinkedHashMap<>();
    private Class<I> inputType;
    private Class<O> outputType;
    private ToolHandler<I, O> handler;

    private ToolBuilder() {
    }

    public static <I, O> ToolBuilder<I, O> create() {
        return new ToolBuilder<>();
    }

    public ToolBuilder<I, O> name(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        return this;
    }

    public ToolBuilder<I, O> displayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        return this;
    }

    public ToolBuilder<I, O> description(String description) {
        this.description = Objects.requireNonNull(description, "description must not be null");
        return this;
    }

    public ToolBuilder<I, O> inputField(String fieldName, String fieldDescription) {
        this.inputFields.put(
                Objects.requireNonNull(fieldName, "fieldName must not be null"),
                Objects.requireNonNull(fieldDescription, "fieldDescription must not be null")
        );
        return this;
    }

    public ToolBuilder<I, O> outputField(String fieldName, String fieldDescription) {
        this.outputFields.put(
                Objects.requireNonNull(fieldName, "fieldName must not be null"),
                Objects.requireNonNull(fieldDescription, "fieldDescription must not be null")
        );
        return this;
    }

    public ToolBuilder<I, O> inputType(Class<I> inputType) {
        this.inputType = Objects.requireNonNull(inputType, "inputType must not be null");
        return this;
    }

    public ToolBuilder<I, O> outputType(Class<O> outputType) {
        this.outputType = Objects.requireNonNull(outputType, "outputType must not be null");
        return this;
    }

    public ToolBuilder<I, O> handler(ToolHandler<I, O> handler) {
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
        return this;
    }

    public ToolRegistration<I, O> build() {
        if (name == null) {
            throw new IllegalStateException("name must be set");
        }
        if (displayName == null) {
            displayName = name;
        }
        if (description == null) {
            description = "";
        }
        if (inputType == null) {
            throw new IllegalStateException("inputType must be set");
        }
        if (outputType == null) {
            throw new IllegalStateException("outputType must be set");
        }
        if (handler == null) {
            throw new IllegalStateException("handler must be set");
        }

        ToolDescriptor descriptor = new ToolDescriptor(
                name,
                displayName,
                description,
                inputFields,
                outputFields
        );

        return new ToolRegistration<>(descriptor, inputType, handler, outputType);
    }
}
