package com.company.assistant.auth;

// C-11 (#85): PUT /admin/users/{id}/roles icin gecersiz calisan id.
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
