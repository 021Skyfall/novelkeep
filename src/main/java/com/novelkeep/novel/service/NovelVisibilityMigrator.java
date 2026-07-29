package com.novelkeep.novel.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class NovelVisibilityMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public NovelVisibilityMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("novel")) {
            return;
        }
        ensureVisibilityColumn();
        ensureRecommendationCountColumn();
        jdbcTemplate.update("""
                UPDATE novel
                   SET status = 'SERIALIZING',
                       visibility = 'PRIVATE'
                 WHERE status = 'DRAFT'
                """);
        jdbcTemplate.update("""
                UPDATE novel
                   SET visibility = 'PUBLIC'
                 WHERE visibility IS NULL OR visibility = ''
                """);
        jdbcTemplate.update("""
                UPDATE novel
                   SET recommendation_count = 0
                 WHERE recommendation_count IS NULL
                """);
    }

    private void ensureVisibilityColumn() {
        if (columnExists("novel", "visibility")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE novel
                ADD COLUMN visibility varchar(20) NOT NULL DEFAULT 'PUBLIC'
                """);
    }

    private void ensureRecommendationCountColumn() {
        if (columnExists("novel", "recommendation_count")) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE novel
                ADD COLUMN recommendation_count bigint NOT NULL DEFAULT 0
                """);
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = ?
                   AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }
}
