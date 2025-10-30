package com.example.mcp.common.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated view of recent invocations used to verify the demo's governance layer.
 */
public final class GovernanceReport {
    private final List<InvocationAuditRecord> records = new ArrayList<>();

    public void addRecord(InvocationAuditRecord record) {
        records.add(record);
    }

    public List<InvocationAuditRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public long countByTool(String toolName) {
        return records.stream().filter(record -> record.getTool().equals(toolName)).count();
    }

    @Override
    public String toString() {
        return "GovernanceReport{" +
                "records=" + records +
                '}';
    }
}
