package com.example.mcp.common.protocol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP 会话握手阶段由客户端发起的请求负载。
 */
public class SessionOpenRequest implements Serializable {
  private String clientId;
  private String clientVersion;
  private List<String> capabilities = new ArrayList<>();

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientVersion() {
    return clientVersion;
  }

  public void setClientVersion(String clientVersion) {
    this.clientVersion = clientVersion;
  }

  public List<String> getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(List<String> capabilities) {
    this.capabilities =
        capabilities == null ? new ArrayList<>() : new ArrayList<>(capabilities);
  }
}
