package com.company.assistant.chatbot;

import java.time.Instant;
import java.util.List;

/**
 * A-22 (#141): {@code actions} ve {@code suggestions} eklendi.
 *
 * <p>Ikisi de her zaman doner (bos liste olabilir), asla null degil — istemcinin
 * "alan var mi" kontrolu yapmasi gerekmesin. Yeni alan eklemek geriye donuk uyumludur.
 *
 * <p>{@code actions}: intent'e bagli yonlendirme butonu, cogu yanitta bostur.
 * {@code suggestions}: yalnizca intent bulunamadiginda dolar — kullanicinin kayboldugu
 * an tam orasi ve karsilama mesaji o noktada coktan ekrandan silinmis olur.
 */
public record ChatMessageResponse(
        String reply,
        String intent,
        Instant timestamp,
        List<ChatAction> actions,
        List<ChatSuggestion> suggestions
) {}
