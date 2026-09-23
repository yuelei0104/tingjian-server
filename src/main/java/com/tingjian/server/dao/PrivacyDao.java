package com.tingjian.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PrivacyDao {
    private final JdbcTemplate jdbc;

    public PrivacyDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int deleteConversationMessages(String ownerId) {
        return jdbc.update("""
                DELETE FROM conversation_message
                WHERE conversation_id IN (SELECT id FROM conversation WHERE owner_id=?)
                """, ownerId);
    }

    public int deleteConversations(String ownerId) {
        return jdbc.update("DELETE FROM conversation WHERE owner_id=?", ownerId);
    }

    public int deleteKeywords(String ownerId) {
        return jdbc.update("DELETE FROM keyword_rule WHERE owner_id=?", ownerId);
    }

    public int deleteGlossaryTerms(String ownerId) {
        return jdbc.update("DELETE FROM glossary_term WHERE owner_id=?", ownerId);
    }

    public int deleteQuickPhrases(String ownerId) {
        return jdbc.update("DELETE FROM quick_phrase WHERE owner_id=?", ownerId);
    }
}
