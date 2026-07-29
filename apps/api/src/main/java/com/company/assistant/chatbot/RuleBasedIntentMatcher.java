package com.company.assistant.chatbot;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.company.assistant.common.TurkishText;

/**
 * A-17 (#124): embedding'den ONCE calisan deterministik intent kurallari.
 *
 * Gerekce: bazi girdiler anlamsal benzerlikle SINIFLANDIRILAMAZ. Olculen ornek —
 * "34 SR 101" sorgusu 0.463 benzerlikle "merhaba" cumlesine en yakin cikiyor; plaka ile
 * servis sorusu arasinda anlamsal bag yok. Ornek cumle eklemek de cozmez, cunku plakalar
 * degistikce model genelleme yapamaz. Yapilandirilmis girdiler (plaka, sicil no, tarih)
 * icin kural katmani her zaman embedding'den guvenilirdir.
 *
 * Kural eslesirse embedding servisi hic cagrilmaz — hem dogru sonuc hem gereksiz LLM/Ollama
 * cagrisinin onlenmesi. Faz 2'de LLM devreye girdiginde de bu katman onde kalir.
 */
@Component
public class RuleBasedIntentMatcher {

    /**
     * Turk plaka bicimi: 2 haneli il kodu + 1-3 harf + 1-5 rakam.
     * Aradaki bosluk opsiyonel, boylece "34 SR 101" ve "34sr101" ayni sekilde yakalanir.
     * Kelime siniri sart: aksi halde uzun rakam dizilerinin icinden yanlis eslesme cikar.
     */
    private static final Pattern PLATE = Pattern.compile("\\b\\d{2}\\s?[a-z]{1,3}\\s?\\d{1,5}\\b");

    /** Kural eslesmesinde chat_message_log'a yazilan etiket; kalibrasyon analizinde ayirt edilsin. */
    static final String RULE_PHRASE = "[kural] plaka";

    /** Plaka bir guzergah/arac bilgisidir; ShuttleVariableResolver bu intent'te plakayi eslestirir. */
    private static final String PLATE_INTENT = "servis_guzergah";

    public Optional<IntentClassificationService.IntentResult> match(String message) {
        String text = TurkishText.foldToAscii(message);
        if (PLATE.matcher(text).find()) {
            return Optional.of(new IntentClassificationService.IntentResult(
                    PLATE_INTENT, 1.0, RULE_PHRASE, true));
        }
        return Optional.empty();
    }
}
