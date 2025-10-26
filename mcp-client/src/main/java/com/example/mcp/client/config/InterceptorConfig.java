package com.example.mcp.client.config;

import java.util.ArrayList;
import java.util.List;

public class InterceptorConfig {
  private String className;
  private List<String> args = new ArrayList<>();

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public List<String> getArgs() {
    return args;
  }

  public void setArgs(List<String> args) {
    this.args = args == null ? new ArrayList<>() : new ArrayList<>(args);
  }
}
