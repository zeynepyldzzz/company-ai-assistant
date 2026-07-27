package com.company.assistant.report;

// C-11 (#85): usage raporundaki tek bir satir (modul + metrik + sayi).
public record UsageReportRow(String module, String metric, long count) {
}
