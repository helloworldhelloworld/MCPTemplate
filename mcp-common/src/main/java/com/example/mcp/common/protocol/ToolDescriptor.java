package com.example.mcp.common.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 描述 MCP 工具在能力发现阶段暴露的元信息。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolDescriptor implements Serializable {
  private String name;
  private String title;
  private String description;
  private String inputSchema;
  private String outputSchema;
  private List<String> capabilities = new ArrayList<>();

  public ToolDescriptor() {}

  public ToolDescriptor(String name, String title, String description) {
    this.name = name;
    this.title = title;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getInputSchema() {
    return inputSchema;
  }

  public void setInputSchema(String inputSchema) {
    this.inputSchema = inputSchema;
  }

  public String getOutputSchema() {
    return outputSchema;
  }

  public void setOutputSchema(String outputSchema) {
    this.outputSchema = outputSchema;
  }

  public List<String> getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(List<String> capabilities) {
    this.capabilities =
        capabilities == null ? new ArrayList<>() : new ArrayList<>(capabilities);
  }

  public ToolDescriptor addCapability(String capability) {
    Objects.requireNonNull(capability, "capability");
    this.capabilities.add(capability);
    return this;
  }
}
