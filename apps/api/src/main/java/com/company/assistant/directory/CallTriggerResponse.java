package com.company.assistant.directory;

import java.time.Instant;

public class CallTriggerResponse {

    private final String extension;
    private final String status;
    private final Instant triggeredAt;

    public CallTriggerResponse(String extension, String status, Instant triggeredAt) {
        this.extension = extension;
        this.status = status;
        this.triggeredAt = triggeredAt;
    }

    public String getExtension() { return extension; }
    public String getStatus() { return status; }
    public Instant getTriggeredAt() { return triggeredAt; }
}
