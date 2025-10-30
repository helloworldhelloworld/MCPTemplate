package com.example.mcp.common.protocol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes a tool's input and output schema in human-readable form.
 */
public final class ToolDescriptor {
    private final String name;
    private final String title;
    private final String description;
    private final Map<String, String> inputSchema;
    private final Map<String, String> outputSchema;

    public ToolDescriptor(String name, String title, String description,
                          Map<String, String> inputSchema, Map<String, String> outputSchema) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.inputSchema = Collections.unmodifiableMap(new LinkedHashMap<>(inputSchema));
        this.outputSchema = Collections.unmodifiableMap(new LinkedHashMap<>(outputSchema));
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getInputSchema() {
        return inputSchema;
    }

    public Map<String, String> getOutputSchema() {
        return outputSchema;
    }

    @Override
    public String toString() {
        return "ToolDescriptor{" +
                "name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", inputSchema=" + inputSchema +
                ", outputSchema=" + outputSchema +
                '}';
    }
}
