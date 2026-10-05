package com.tingjian.server.dao;

import com.tingjian.server.entity.AccessibilityPreferenceEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class AccessibilityPreferenceDao {
    private final JdbcTemplate jdbc;

    public AccessibilityPreferenceDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AccessibilityPreferenceEntity> find(String ownerId) {
        return jdbc.query("""
                SELECT owner_id, high_contrast, visual_alerts, system_notifications,
                       strong_vibration, caption_follow, updated_at
                FROM accessibility_preference
                WHERE owner_id=?
                """, this::mapRow, ownerId).stream().findFirst();
    }

    public void upsert(AccessibilityPreferenceEntity entity) {
        jdbc.update("""
                INSERT INTO accessibility_preference
                    (owner_id, high_contrast, visual_alerts, system_notifications,
                     strong_vibration, caption_follow, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    high_contrast=VALUES(high_contrast),
                    visual_alerts=VALUES(visual_alerts),
                    system_notifications=VALUES(system_notifications),
                    strong_vibration=VALUES(strong_vibration),
                    caption_follow=VALUES(caption_follow),
                    updated_at=VALUES(updated_at)
                """, entity.ownerId(), entity.highContrast(), entity.visualAlerts(),
                entity.systemNotifications(), entity.strongVibration(),
                entity.captionFollow(), entity.updatedAt());
    }

    private AccessibilityPreferenceEntity mapRow(ResultSet rs, int row) throws SQLException {
        return new AccessibilityPreferenceEntity(
                rs.getString("owner_id"), rs.getBoolean("high_contrast"),
                rs.getBoolean("visual_alerts"), rs.getBoolean("system_notifications"),
                rs.getBoolean("strong_vibration"), rs.getBoolean("caption_follow"),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
