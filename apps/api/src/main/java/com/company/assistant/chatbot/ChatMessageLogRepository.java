package com.company.assistant.chatbot;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatMessageLogRepository {

    private static final String INSERT_SQL = """
            INSERT INTO chat_message_log
                (question, intent, similarity, matched_phrase, matched, threshold, response_time_ms)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public ChatMessageLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(ChatMessageLogEntry entry) {
        jdbcTemplate.update(INSERT_SQL,
                entry.question(),
                entry.intent(),
                entry.similarity(),
                entry.matchedPhrase(),
                entry.matched(),
                entry.threshold(),
                entry.responseTimeMs());
    }
}