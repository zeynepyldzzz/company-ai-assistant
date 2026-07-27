package com.company.assistant.report;

import java.time.Instant;
import java.util.List;

// C-11 (#85): GET /admin/reports/usage yaniti.
public record UsageReportResponse(String type, Instant generatedAt, List<UsageReportRow> rows) {
}
