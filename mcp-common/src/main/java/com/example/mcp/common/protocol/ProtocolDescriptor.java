package com.example.mcp.common.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * 描述 MCP 服务在会话握手阶段公布的协议路由信息。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProtocolDescriptor implements Serializable {
  private String session;
  private String invoke;
  private String stream;
  private String discovery;
  private String governance;

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public String getInvoke() {
    return invoke;
  }

  public void setInvoke(String invoke) {
    this.invoke = invoke;
  }

  public String getStream() {
    return stream;
  }

  public void setStream(String stream) {
    this.stream = stream;
  }

  public String getDiscovery() {
    return discovery;
  }

  public void setDiscovery(String discovery) {
    this.discovery = discovery;
  }

  public String getGovernance() {
    return governance;
  }

  public void setGovernance(String governance) {
    this.governance = governance;
  }
}
