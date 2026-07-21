package com.company.assistant.chatbot;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);
    private static final long NFR02_LIMIT_MS = 5000;

    private final IntentClassificationService classificationService;
    private final TemplateResponseService templateResponseService;
    private final ChatVariableResolver variableResolver;

    public ChatMessageService(IntentClassificationService classificationService,
                              TemplateResponseService templateResponseService,
                              ChatVariableResolver variableResolver) {
        this.classificationService = classificationService;
        this.templateResponseService = templateResponseService;
        this.variableResolver = variableResolver;
    }

    public ChatMessageResponse handleMessage(String message, Authentication authentication) {
        long start = System.nanoTime();

        IntentClassificationService.IntentResult result =
                classificationService.classify(message);
        String reply = templateResponseService.buildResponse(
                result.intent(), variableResolver.resolve(authentication));

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        if (elapsedMs > NFR02_LIMIT_MS) {
            log.warn("NFR-02 aşıldı: chatbot yanıtı {} ms sürdü (intent={})",
                    elapsedMs, result.intent());
        } else {
            log.debug("Chatbot yanıtı {} ms (intent={})", elapsedMs, result.intent());
        }

        return new ChatMessageResponse(reply, result.intent(), Instant.now());
    }
}