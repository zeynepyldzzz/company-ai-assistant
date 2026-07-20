package com.company.assistant.menu;

public class MenuImportException extends RuntimeException {
    public MenuImportException(String message) {
        super(message);
    }

    public MenuImportException(String message, Throwable cause) {
        super(message, cause);
    }
}