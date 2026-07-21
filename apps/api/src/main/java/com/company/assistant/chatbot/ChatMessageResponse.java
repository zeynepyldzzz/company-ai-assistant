package com.company.assistant.chatbot;

import java.time.Instant;

public record ChatMessageResponse(
        String reply,
        String intent,
        Instant timestamp
) {}