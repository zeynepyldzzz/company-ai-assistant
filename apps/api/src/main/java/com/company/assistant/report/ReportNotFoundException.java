package com.company.assistant.report;

// C-11 (#85): bilinmeyen rapor tipi istendiginde (MVP'de yalnizca "usage" var).
public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(String message) {
        super(message);
    }
}
