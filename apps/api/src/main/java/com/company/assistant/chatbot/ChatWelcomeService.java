package com.company.assistant.chatbot;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * A-22 (#141): sohbet acilis karsilamasi.
 *
 * <p>Karsilama metni AYRI bir yerde tutulmaz: {@code selamlama} intent'inin template'i
 * kullanilir. "Merhaba" yazan kullaniciya donen cevap ile acilista gosterilen metin ayni
 * seydir; iki kopya tutmak birinin eskimesi demektir — A-20'de tam olarak bunu yasadik,
 * selamlama metni aylar once eklenen yeteneklerden habersiz kalmisti.
 *
 * <p>Yan fayda: {@code {{kullanici_adi}}} degiskeni mevcut zincirden bedavaya geliyor.
 */
@Service
public class ChatWelcomeService {

    private static final String GREETING_INTENT = "selamlama";

    private final TemplateResponseService templateResponseService;
    private final ChatVariableResolver variableResolver;
    private final IntentSuggestionRepository suggestionRepository;

    public ChatWelcomeService(TemplateResponseService templateResponseService,
                              ChatVariableResolver variableResolver,
                              IntentSuggestionRepository suggestionRepository) {
        this.templateResponseService = templateResponseService;
        this.variableResolver = variableResolver;
        this.suggestionRepository = suggestionRepository;
    }

    public ChatWelcomeResponse buildWelcome(Authentication authentication) {
        String message = templateResponseService.buildResponse(
                GREETING_INTENT, variableResolver.resolve(authentication));
        return new ChatWelcomeResponse(message, suggestionRepository.findSuggestions());
    }
}
