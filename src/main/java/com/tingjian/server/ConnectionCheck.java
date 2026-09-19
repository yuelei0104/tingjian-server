package com.tingjian.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("connection-check")
public class ConnectionCheck implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public ConnectionCheck(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1", Integer.class);

        if (!Integer.valueOf(1).equals(result)) {
            throw new IllegalStateException("MySQL 连接检查失败");
        }
        System.out.println("【连接检查】MySQL：成功");

        String pong = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<String>)
                        connection -> connection.ping());

        if (!"PONG".equalsIgnoreCase(pong)) {
            throw new IllegalStateException("Redis 连接检查失败");
        }
        System.out.println("【连接检查】Redis：成功");
    }
}