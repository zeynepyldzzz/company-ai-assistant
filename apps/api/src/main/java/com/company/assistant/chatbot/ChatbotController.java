package com.company.assistant.chatbot;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatMessageService chatMessageService;

    public ChatbotController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@Valid @RequestBody ChatMessageRequest request,
                                           Authentication authentication) {
        return chatMessageService.handleMessage(request.message(), authentication);
    }
}