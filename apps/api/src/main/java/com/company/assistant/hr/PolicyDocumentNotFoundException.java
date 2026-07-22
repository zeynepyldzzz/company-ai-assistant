package com.company.assistant.hr;

// Bulunamayan veya soft-delete edilmis policy_document icin firlatilir; controller-local
// @ExceptionHandler bunu 404 + ErrorResponse'a cevirir.
public class PolicyDocumentNotFoundException extends RuntimeException {

    public PolicyDocumentNotFoundException(String message) {
        super(message);
    }
}
