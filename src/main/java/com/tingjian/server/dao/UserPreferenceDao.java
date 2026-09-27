package com.tingjian.server.dao;

import com.tingjian.server.entity.UserPreferenceEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserPreferenceDao {
    private final JdbcTemplate jdbc;

    public UserPreferenceDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserPreferenceEntity> find(String ownerId) {
        return jdbc.query("""
                SELECT owner_id, large_text, voice_mode, voice_style, tts_speed,
                       recognition_language, keyword_vibration, keyword_highlight,
                       auto_summary, updated_at
                FROM user_preference
                WHERE owner_id=?
                """, this::mapRow, ownerId).stream().findFirst();
    }

    public void upsert(UserPreferenceEntity entity) {
        jdbc.update("""
                INSERT INTO user_preference
                    (owner_id, large_text, voice_mode, voice_style, tts_speed,
                     recognition_language, keyword_vibration, keyword_highlight,
                     auto_summary, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    large_text=VALUES(large_text), voice_mode=VALUES(voice_mode),
                    voice_style=VALUES(voice_style), tts_speed=VALUES(tts_speed),
                    recognition_language=VALUES(recognition_language),
                    keyword_vibration=VALUES(keyword_vibration),
                    keyword_highlight=VALUES(keyword_highlight),
                    auto_summary=VALUES(auto_summary), updated_at=VALUES(updated_at)
                """, entity.ownerId(), entity.largeText(), entity.voiceMode(),
                entity.voiceStyle(), entity.ttsSpeed(), entity.recognitionLanguage(),
                entity.keywordVibration(), entity.keywordHighlight(), entity.autoSummary(),
                entity.updatedAt());
    }

    private UserPreferenceEntity mapRow(ResultSet rs, int row) throws SQLException {
        return new UserPreferenceEntity(
                rs.getString("owner_id"), rs.getBoolean("large_text"),
                rs.getString("voice_mode"), rs.getString("voice_style"),
                rs.getDouble("tts_speed"), rs.getString("recognition_language"),
                rs.getBoolean("keyword_vibration"), rs.getBoolean("keyword_highlight"),
                rs.getBoolean("auto_summary"),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
