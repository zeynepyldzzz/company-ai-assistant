package com.company.assistant.hr;

// Gecersiz topic veya bulunamayan id icin firlatilir; controller-local @ExceptionHandler
// bunu 404 + ErrorResponse'a cevirir (DirectoryController deseni).
public class HrProcedureNotFoundException extends RuntimeException {

    public HrProcedureNotFoundException(String message) {
        super(message);
    }
}
