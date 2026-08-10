package com.company.assistant.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank(message = "Mesaj boş olamaz")
        @Size(max = 1000, message = "Mesaj 1000 karakteri aşamaz")
        String message,

        /**
         * A-37 (#203): onceki yanitin intent'i — "peki yarin?" tipi takip sorulari icin.
         *
         * <p>OPSIYONEL: gonderilmezse davranis eskisi gibi kalir. Sunucu sohbet durumu
         * tutmuyor ({@code chat_message_log}'da kimlik yok, V13 ekip karari), bu yuzden
         * baglami istemci tasiyor.
         *
         * <p>Uzunluk siniri, {@code intents.name} kolonuyla ayni (64).
         */
        @Size(max = 64, message = "Intent adı 64 karakteri aşamaz")
        String previousIntent
) {}
