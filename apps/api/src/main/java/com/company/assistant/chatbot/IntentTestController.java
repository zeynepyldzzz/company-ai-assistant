package com.company.assistant.chatbot;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GECICI — A-1 dev/test amacli. A-3'te POST /chatbot/messages
 * gelince kaldirilacak ya da ic servise donusecek.
 */
@RestController
@RequestMapping("/chatbot")
public class IntentTestController {

    private final IntentClassificationService classificationService;

    public IntentTestController(IntentClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping("/classify")
    public IntentClassificationService.IntentResult classify(@RequestBody ClassifyRequest request) {
        return classificationService.classify(request.question());
    }

    public record ClassifyRequest(String question) {}
}