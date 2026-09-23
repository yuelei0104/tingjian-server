package com.tingjian.server.dao;

import com.tingjian.server.entity.UserEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserDao {
    private static final String SELECT_COLUMNS = """
            SELECT id, email, password_hash, display_name, status, created_at, updated_at
            FROM app_user
            """;

    private final JdbcTemplate jdbc;

    public UserDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(UserEntity entity) {
        jdbc.update("""
                INSERT INTO app_user
                    (id, email, password_hash, display_name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, entity.id(), entity.email(), entity.passwordHash(), entity.displayName(),
                entity.status(), entity.createdAt(), entity.updatedAt());
    }

    public Optional<UserEntity> findByEmail(String email) {
        return jdbc.query(SELECT_COLUMNS + "WHERE email=?", this::mapRow, email).stream().findFirst();
    }

    public Optional<UserEntity> findById(String id) {
        return jdbc.query(SELECT_COLUMNS + "WHERE id=?", this::mapRow, id).stream().findFirst();
    }

    private UserEntity mapRow(ResultSet rs, int row) throws SQLException {
        return new UserEntity(
                rs.getString("id"), rs.getString("email"), rs.getString("password_hash"),
                rs.getString("display_name"), rs.getString("status"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
