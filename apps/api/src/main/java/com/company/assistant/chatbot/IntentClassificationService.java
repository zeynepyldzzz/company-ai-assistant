package com.company.assistant.chatbot; // kendi kökünüze göre

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.company.assistant.common.TurkishText;

@Service
public class IntentClassificationService {

    private static final Logger log = LoggerFactory.getLogger(IntentClassificationService.class);

    public static final String NO_INTENT = "intent_bulunamadi";

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;
    private final RuleBasedIntentMatcher ruleMatcher;
    private final double similarityThreshold;

    public IntentClassificationService(
            JdbcTemplate jdbcTemplate,
            EmbeddingClient embeddingClient,
            RuleBasedIntentMatcher ruleMatcher,
            @Value("${app.chatbot.similarity-threshold:0.65}") double similarityThreshold) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
        this.ruleMatcher = ruleMatcher;
        this.similarityThreshold = similarityThreshold;
    }

    public IntentResult classify(String question) {
        // A-17 (#124): yapilandirilmis girdiler (plaka gibi) anlamsal benzerlikle
        // siniflandirilamaz — "34 SR 101" 0.463 ile "merhaba"ya en yakin cikiyordu.
        // Kural eslesirse embedding hic cagrilmaz.
        Optional<IntentResult> ruleMatch = ruleMatcher.match(question);
        if (ruleMatch.isPresent()) {
            return ruleMatch.get();
        }

        float[] queryVector;
        try {
            // A-17 (#124): sorgu ve seed AYNI normalizasyondan gecmeli. Normalizasyon yokken
            // "Selam" 0.512 / "selam" 0.787 aliyordu; buyuk harfle baslayan sorular esik
            // altinda kaliyordu.
            queryVector = embeddingClient.embed(TurkishText.normalizeForEmbedding(question));
        } catch (Exception e) {
            log.error("Embedding servisi erisilemedi, fallback donuluyor: {}", e.getMessage());
            return IntentResult.noMatch();
        }

        String vectorLiteral = IntentSeedRunner.toVectorLiteral(queryVector);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT i.name,
                       e.phrase,
                       1 - (e.embedding <=> CAST(? AS vector)) AS similarity
                FROM intent_examples e
                JOIN intents i ON i.id = e.intent_id
                WHERE e.embedding IS NOT NULL
                 AND i.is_virtual = false
                ORDER BY e.embedding <=> CAST(? AS vector)
                LIMIT 1
                """, vectorLiteral, vectorLiteral);

        if (rows.isEmpty()) {
            log.warn("intent_examples bos — seed calisti mi?");
            return IntentResult.noMatch();
        }

        Map<String, Object> best = rows.get(0);
        String intentName = (String) best.get("name");
        String matchedPhrase = (String) best.get("phrase");
        double similarity = ((Number) best.get("similarity")).doubleValue();

        log.debug("Siniflandirma: soru='{}' -> intent='{}' (ornek='{}', benzerlik={})",
                question, intentName, matchedPhrase, similarity);

        if (similarity < similarityThreshold) {
            return new IntentResult(NO_INTENT, similarity, matchedPhrase, false);
        }
        return new IntentResult(intentName, similarity, matchedPhrase, true);
    }

    public record IntentResult(String intent, double similarity, String matchedPhrase, boolean matched) {
        static IntentResult noMatch() {
            return new IntentResult(NO_INTENT, 0.0, null, false);
        }
    }
    public double getThreshold() {
    return this.similarityThreshold;   
}
}