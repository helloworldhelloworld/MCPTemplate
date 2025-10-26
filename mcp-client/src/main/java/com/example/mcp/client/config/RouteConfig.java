package com.example.mcp.client.config;

import java.util.Optional;

public class RouteConfig {
  private String server;
  private String tool;
  private String requestType;
  private String responseType;
  private String requestEvent;
  private String responseEvent;
  private String errorEvent;

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public String getTool() {
    return tool;
  }

  public void setTool(String tool) {
    this.tool = tool;
  }

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public String getResponseType() {
    return responseType;
  }

  public void setResponseType(String responseType) {
    this.responseType = responseType;
  }

  public String getRequestEvent() {
    return requestEvent;
  }

  public void setRequestEvent(String requestEvent) {
    this.requestEvent = requestEvent;
  }

  public String getResponseEvent() {
    return responseEvent;
  }

  public void setResponseEvent(String responseEvent) {
    this.responseEvent = responseEvent;
  }

  public String getErrorEvent() {
    return errorEvent;
  }

  public void setErrorEvent(String errorEvent) {
    this.errorEvent = errorEvent;
  }

  public String resolveRequestEvent(String routeName) {
    return Optional.ofNullable(requestEvent).orElse(routeName + ".request");
  }

  public String resolveResponseEvent(String routeName) {
    return Optional.ofNullable(responseEvent).orElse(routeName + ".response");
  }

  public String resolveErrorEvent(String routeName) {
    return Optional.ofNullable(errorEvent).orElse(routeName + ".error");
  }
}
