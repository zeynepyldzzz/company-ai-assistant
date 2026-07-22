package com.company.assistant.chatbot;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class IntentSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IntentSeedRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;

    public IntentSeedRunner(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> pending = jdbcTemplate.queryForList("""
                SELECT e.id, e.phrase FROM intent_examples e
                JOIN intents i ON i.id = e.intent_id
                WHERE embedding IS NULL 
                 AND i.is_virtual = false
                """);

        if (pending.isEmpty()) {
            log.info("Intent embedding seed: bekleyen kayit yok, atlandi.");
            return;
        }

        log.info("Intent embedding seed: {} kayit icin embedding hesaplanacak...", pending.size());

        int done = 0;
        for (Map<String, Object> row : pending) {
            Long id = ((Number) row.get("id")).longValue();
            String phrase = (String) row.get("phrase");
            try {
                float[] vector = embeddingClient.embed(phrase);
                jdbcTemplate.update(
                        "UPDATE intent_examples SET embedding = CAST(? AS vector) WHERE id = ?",
                        toVectorLiteral(vector), id);
                done++;
            } catch (Exception e) {
                log.error("Embedding hesaplanamadi (id={}, phrase='{}'): {}", id, phrase, e.getMessage());
            }
        }

        log.info("Intent embedding seed tamamlandi: {}/{} kayit.", done, pending.size());
    }

    static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}