package com.company.assistant.chatbot;

public record ChatMessageLogEntry(
        String question,
        String intent,
        double similarity,
        String matchedPhrase,
        boolean matched,
        double threshold,
        Integer responseTimeMs
) {}