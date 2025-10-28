package com.example.mcp.server.service;

import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.InvocationAuditRecord;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InvocationAuditService {
  private final List<InvocationAuditRecord> records = new CopyOnWriteArrayList<>();

  public void record(InvocationAuditRecord record) {
    if (record != null) {
      records.add(record);
    }
  }

  public GovernanceReport findByRequestId(String requestId) {
    GovernanceReport report = new GovernanceReport();
    report.setEvents(
        records.stream()
            .filter(record -> requestId == null || requestId.equals(record.getRequestId()))
            .collect(Collectors.toList()));
    return report;
  }

  public void clear() {
    records.clear();
  }
}
