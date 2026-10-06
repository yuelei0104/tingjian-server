package com.tingjian.server.dao;

import com.tingjian.server.entity.AuthSessionMetadataEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class AuthSessionMetadataDao {
    private final JdbcTemplate jdbc;

    public AuthSessionMetadataDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(AuthSessionMetadataEntity entity) {
        jdbc.update("""
                INSERT INTO auth_session_metadata
                    (session_id, user_id, device_id_hash, device_name, platform,
                     app_version, ip_address, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, entity.sessionId(), entity.userId(), entity.deviceIdHash(),
                entity.deviceName(), entity.platform(), entity.appVersion(),
                entity.ipAddress(), entity.createdAt());
    }

    public Optional<AuthSessionMetadataEntity> findBySessionId(String sessionId) {
        return jdbc.query("""
                SELECT session_id, user_id, device_id_hash, device_name, platform,
                       app_version, ip_address, created_at
                FROM auth_session_metadata WHERE session_id=?
                """, (rs, row) -> new AuthSessionMetadataEntity(
                rs.getString("session_id"), rs.getString("user_id"),
                rs.getString("device_id_hash"), rs.getString("device_name"),
                rs.getString("platform"), rs.getString("app_version"),
                rs.getString("ip_address"),
                rs.getObject("created_at", LocalDateTime.class)), sessionId).stream().findFirst();
    }

    public boolean isKnownDevice(String userId, String deviceIdHash) {
        if (deviceIdHash == null || deviceIdHash.isBlank()) {
            return true;
        }
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM auth_session_metadata
                WHERE user_id=? AND device_id_hash=?
                """, Long.class, userId, deviceIdHash);
        return count != null && count > 0;
    }
}
