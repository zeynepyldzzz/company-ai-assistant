package com.company.assistant.chatbot; 

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class ResponseTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    public ResponseTemplateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findEnabledTemplateByIntentName(String intentName) {
        List<String> results = jdbcTemplate.query("""
                SELECT rt.template
                FROM response_templates rt
                JOIN intents i ON i.id = rt.intent_id
                WHERE i.name = ? AND rt.enabled = true
                """, (rs, rowNum) -> rs.getString("template"), intentName);
        return results.stream().findFirst();
    }
}