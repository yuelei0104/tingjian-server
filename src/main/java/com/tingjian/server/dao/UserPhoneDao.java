package com.tingjian.server.dao;

import com.tingjian.server.entity.UserPhoneEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserPhoneDao {
    private final JdbcTemplate jdbc;

    public UserPhoneDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserPhoneEntity> findByUserId(String userId) {
        return jdbc.query("""
                SELECT user_id, phone, verified_at, updated_at
                FROM user_phone WHERE user_id=?
                """, (rs, row) -> new UserPhoneEntity(
                rs.getString("user_id"), rs.getString("phone"),
                rs.getObject("verified_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class)), userId).stream().findFirst();
    }

    public Optional<String> findUserIdByPhone(String phone) {
        return jdbc.query("SELECT user_id FROM user_phone WHERE phone=?",
                (rs, row) -> rs.getString(1), phone).stream().findFirst();
    }

    public void upsert(String userId, String phone, LocalDateTime now) {
        int updated = jdbc.update("""
                UPDATE user_phone SET phone=?, verified_at=?, updated_at=? WHERE user_id=?
                """, phone, now, now, userId);
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO user_phone (user_id, phone, verified_at, updated_at)
                    VALUES (?, ?, ?, ?)
                    """, userId, phone, now, now);
        }
    }

    public int deleteByUserId(String userId) {
        return jdbc.update("DELETE FROM user_phone WHERE user_id=?", userId);
    }
}
