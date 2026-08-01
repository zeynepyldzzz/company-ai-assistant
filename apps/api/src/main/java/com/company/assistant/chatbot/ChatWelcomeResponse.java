package com.company.assistant.chatbot;

import java.util.List;

/**
 * A-22 (#141): sohbet acilisinda gosterilen karsilama.
 *
 * <p>Ayri bir uc olmasinin sebebi: karsilama kullanici HENUZ mesaj gondermeden gerekli,
 * dolayisiyla POST /chatbot/messages yanitina gomulemez.
 *
 * <p>Icerik burada TUTULMAZ, uretilir: metin selamlama template'inden, oneriler
 * intents tablosundan gelir. Bu uc bir kopru; ikinci bir icerik deposu degil.
 */
public record ChatWelcomeResponse(
        String message,
        List<ChatSuggestion> suggestions
) {}
