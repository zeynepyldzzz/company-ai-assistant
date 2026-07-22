package com.company.assistant.chatbot;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TemplateResponseService {

    private static final Logger log = LoggerFactory.getLogger(TemplateResponseService.class);
    private static final String FALLBACK_INTENT = "intent_bulunamadi";
    private static final String LAST_RESORT =
            "Üzgünüm, şu anda bu soruya yanıt veremiyorum. Lütfen daha sonra tekrar dene.";

    private final ResponseTemplateRepository templateRepository;
    private final TemplateRenderer renderer;

    public TemplateResponseService(ResponseTemplateRepository templateRepository,
                                   TemplateRenderer renderer) {
        this.templateRepository = templateRepository;
        this.renderer = renderer;
    }

    public String buildResponse(String intentName, Map<String, String> variables) {
        String template = templateRepository.findEnabledTemplateByIntentName(intentName)
                .or(() -> {
                    log.warn("Intent '{}' icin template yok/kapali, fallback deneniyor.", intentName);
                    return templateRepository.findEnabledTemplateByIntentName(FALLBACK_INTENT);
                })
                .orElseGet(() -> {
                    log.error("Fallback template de bulunamadi! Son care yanit donuluyor.");
                    return LAST_RESORT;
                });
        return renderer.render(template, variables);
    }

    /**
     * İK prosedurunun guncel versiyonu bulunamadiginda (dokuman §2) dogrudan fallback
     * ('intent_bulunamadi') template'ini render eder. Boylece cozulmemis {{...}}
     * placeholder'lari kullaniciya sizmaz. FALLBACK_INTENT sabiti bu sinifta kapsullu kalir.
     */
    public String buildFallbackResponse(Map<String, String> variables) {
        String template = templateRepository.findEnabledTemplateByIntentName(FALLBACK_INTENT)
                .orElseGet(() -> {
                    log.error("Fallback template de bulunamadi! Son care yanit donuluyor.");
                    return LAST_RESORT;
                });
        return renderer.render(template, variables);
    }
}