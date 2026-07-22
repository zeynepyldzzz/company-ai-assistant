package com.company.assistant.shuttle;

public class NoShuttleRecommendationException extends RuntimeException {
    public NoShuttleRecommendationException(String message) {
        super(message);
    }
}
