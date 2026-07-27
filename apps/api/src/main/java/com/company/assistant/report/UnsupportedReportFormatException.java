package com.company.assistant.report;

// C-11 (#85): export icin desteklenmeyen format (MVP'de yalnizca xlsx).
public class UnsupportedReportFormatException extends RuntimeException {
    public UnsupportedReportFormatException(String message) {
        super(message);
    }
}
