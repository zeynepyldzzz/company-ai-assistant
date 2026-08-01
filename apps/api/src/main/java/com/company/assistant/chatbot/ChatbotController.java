package com.company.assistant.chatbot;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatMessageService chatMessageService;
    private final ChatWelcomeService chatWelcomeService;

    public ChatbotController(ChatMessageService chatMessageService,
                             ChatWelcomeService chatWelcomeService) {
        this.chatMessageService = chatMessageService;
        this.chatWelcomeService = chatWelcomeService;
    }

    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@Valid @RequestBody ChatMessageRequest request,
                                           Authentication authentication) {
        return chatMessageService.handleMessage(request.message(), authentication);
    }

    /**
     * A-22 (#141): sohbet acilisinda gosterilen karsilama. Ayri bir uc, cunku kullanici
     * henuz mesaj gondermeden gerekli. SecurityConfig'e satir eklenmez —
     * anyRequest().authenticated() varsayilani bu ucu zaten koruyor.
     */
    @GetMapping("/welcome")
    public ChatWelcomeResponse welcome(Authentication authentication) {
        return chatWelcomeService.buildWelcome(authentication);
    }
}