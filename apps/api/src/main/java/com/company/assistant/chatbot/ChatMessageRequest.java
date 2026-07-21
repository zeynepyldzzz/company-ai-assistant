package com.company.assistant.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "Mesaj boş olamaz")
        @Size(max = 1000, message = "Mesaj 1000 karakteri aşamaz")
        String message
) {}