package com.company.assistant.shuttle;

public class ShuttleRouteNotFoundException extends RuntimeException {
    public ShuttleRouteNotFoundException(String message) {
        super(message);
    }
}
