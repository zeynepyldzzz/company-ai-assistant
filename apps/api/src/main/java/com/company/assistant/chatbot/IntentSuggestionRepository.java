package com.company.assistant.chatbot;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * A-22 (#141): intent'in KULLANICIYA donuk metadatasi — vitrin sorusu ve yonlendirme butonu.
 *
 * <p>ResponseTemplateRepository deseni: JdbcTemplate, JPA yok (CLAUDE.md).
 *
 * <p>Bu veri neden intents tablosunda: welcome ucu bir kopru olsun, ikinci bir icerik
 * listesi (kod ici sabit dizi ya da ayri tablo) dogmasin diye. Yeni bir intent eklendiginde
 * vitrin sorusu ve butonu ayni migration'da tanimlanir; senkronlanacak baska yer yok.
 */
@Repository
public class IntentSuggestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public IntentSuggestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Vitrine cikan intent'ler. is_virtual filtresi DB kisitiyla zaten garanti altinda
     * (sanal intent'e vitrin sorusu yazilamiyor), yine de sorguda acikca belirtiliyor:
     * kisit ileride gevsetilirse bu sorgu sessizce "anlamadim" chip'i uretmesin.
     */
    public List<ChatSuggestion> findSuggestions() {
        return jdbcTemplate.query("""
                SELECT suggested_question
                FROM intents
                WHERE suggested_question IS NOT NULL AND is_virtual = false
                ORDER BY suggestion_order
                """, (rs, rowNum) -> {
            String question = rs.getString("suggested_question");
            return new ChatSuggestion(question, question);
        });
    }

    /** Intent'in yonlendirme butonu; cogu intent'te yoktur ve olmamasi normaldir. */
    public Optional<ChatAction> findActionByIntentName(String intentName) {
        return jdbcTemplate.query("""
                SELECT action_target, action_label
                FROM intents
                WHERE name = ? AND action_target IS NOT NULL
                """, (rs, rowNum) -> new ChatAction(
                        rs.getString("action_target"),
                        rs.getString("action_label")),
                intentName).stream().findFirst();
    }
}
