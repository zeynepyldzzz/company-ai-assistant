package com.company.assistant.chatbot;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

/**
 * GECICI — A-1 dev/test amacli. A-3'te POST /chatbot/messages gelince
 * kaldirilacak ya da ic servise donusecek.
 */
@RestController
@RequestMapping("/chatbot")
public class IntentTestController {

    private final IntentClassificationService classificationService;
    private final TemplateResponseService templateResponseService;
    private final ChatVariableResolver variableResolver;

    public IntentTestController(IntentClassificationService classificationService,
                            TemplateResponseService templateResponseService,
                            ChatVariableResolver variableResolver) {
        this.classificationService = classificationService;
        this.templateResponseService = templateResponseService;
        this.variableResolver = variableResolver;
    }

    @PostMapping("/classify")
    public ClassifyResponse classify(@RequestBody ClassifyRequest request,
            Authentication authentication) {
        IntentClassificationService.IntentResult result
                = classificationService.classify(request.question());
        String response = templateResponseService.buildResponse(
                result.intent(), variableResolver.resolve(authentication));
        return new ClassifyResponse(result, response);
    }

    public record ClassifyRequest(String question) {

    }

    public record ClassifyResponse(IntentClassificationService.IntentResult classification,
            String response) {

    }
}
