package com.example.mcp.client.config;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {
  private TransportType type = TransportType.HTTP;
  private String baseUrl;
  private String host;
  private Integer port;
  private boolean plaintext = true;
  private String sdkClass;
  private List<InterceptorConfig> interceptors = new ArrayList<>();
  private String invokePath;
  private String streamPath;
  private String sessionPath;
  private String discoveryPath;
  private String governancePath;

  private static final String DEFAULT_INVOKE_PATH = "/mcp/invoke";
  private static final String DEFAULT_STREAM_PATH = "/mcp/stream";
  private static final String DEFAULT_SESSION_PATH = "/mcp/session";
  private static final String DEFAULT_DISCOVERY_PATH = "/mcp/tools";
  private static final String DEFAULT_GOVERNANCE_PATH = "/mcp/governance/audit";

  public TransportType getType() {
    return type;
  }

  public void setType(TransportType type) {
    this.type = type;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public boolean isPlaintext() {
    return plaintext;
  }

  public void setPlaintext(boolean plaintext) {
    this.plaintext = plaintext;
  }

  public String getSdkClass() {
    return sdkClass;
  }

  public void setSdkClass(String sdkClass) {
    this.sdkClass = sdkClass;
  }

  public List<InterceptorConfig> getInterceptors() {
    return interceptors;
  }

  public void setInterceptors(List<InterceptorConfig> interceptors) {
    this.interceptors =
        interceptors == null ? new ArrayList<>() : new ArrayList<>(interceptors);
  }

  public String getInvokePath() {
    return invokePath;
  }

  public void setInvokePath(String invokePath) {
    this.invokePath = invokePath;
  }

  public String getStreamPath() {
    return streamPath;
  }

  public void setStreamPath(String streamPath) {
    this.streamPath = streamPath;
  }

  public String getSessionPath() {
    return sessionPath;
  }

  public void setSessionPath(String sessionPath) {
    this.sessionPath = sessionPath;
  }

  public String getDiscoveryPath() {
    return discoveryPath;
  }

  public void setDiscoveryPath(String discoveryPath) {
    this.discoveryPath = discoveryPath;
  }

  public String getGovernancePath() {
    return governancePath;
  }

  public void setGovernancePath(String governancePath) {
    this.governancePath = governancePath;
  }

  public String resolveInvokePath() {
    return defaultIfBlank(invokePath, DEFAULT_INVOKE_PATH);
  }

  public String resolveStreamPath() {
    return defaultIfBlank(streamPath, DEFAULT_STREAM_PATH);
  }

  public String resolveSessionPath() {
    return defaultIfBlank(sessionPath, DEFAULT_SESSION_PATH);
  }

  public String resolveDiscoveryPath() {
    return defaultIfBlank(discoveryPath, DEFAULT_DISCOVERY_PATH);
  }

  public String resolveGovernancePath() {
    return defaultIfBlank(governancePath, DEFAULT_GOVERNANCE_PATH);
  }

  private String defaultIfBlank(String value, String defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value;
  }
}
