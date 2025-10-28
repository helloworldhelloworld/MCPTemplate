package com.example.mcp.common.protocol;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 治理层返回的审计报告，聚合多条审计记录。
 */
public class GovernanceReport implements Serializable {
  private List<InvocationAuditRecord> events = new ArrayList<>();

  public List<InvocationAuditRecord> getEvents() {
    return events;
  }

  public void setEvents(List<InvocationAuditRecord> events) {
    this.events = events == null ? new ArrayList<>() : new ArrayList<>(events);
  }
}
