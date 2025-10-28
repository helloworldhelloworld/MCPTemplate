package com.example.mcp.common.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端在 MCP 会话握手阶段返回的响应体。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionOpenResponse implements Serializable {
  private String sessionId;
  private String serverName;
  private String serverVersion;
  private OffsetDateTime expiresAt;
  private List<ToolDescriptor> tools = new ArrayList<>();
  private ProtocolDescriptor protocol;

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public String getServerVersion() {
    return serverVersion;
  }

  public void setServerVersion(String serverVersion) {
    this.serverVersion = serverVersion;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public List<ToolDescriptor> getTools() {
    return tools;
  }

  public void setTools(List<ToolDescriptor> tools) {
    this.tools = tools == null ? new ArrayList<>() : new ArrayList<>(tools);
  }

  public ProtocolDescriptor getProtocol() {
    return protocol;
  }

  public void setProtocol(ProtocolDescriptor protocol) {
    this.protocol = protocol;
  }
}
