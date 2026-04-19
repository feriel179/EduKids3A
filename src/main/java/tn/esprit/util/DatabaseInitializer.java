package tn.esprit.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize(Connection cnx) {
        try (Statement statement = cnx.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS cours (
                        id INT NOT NULL AUTO_INCREMENT,
                        titre VARCHAR(255) NOT NULL,
                        description VARCHAR(255) NOT NULL,
                        niveau INT NOT NULL,
                        matiere VARCHAR(255) NOT NULL,
                        image VARCHAR(255) NOT NULL,
                        likes INT NOT NULL DEFAULT 0,
                        dislikes INT NOT NULL DEFAULT 0,
                        status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                        lesson_count INT NOT NULL DEFAULT 0,
                        total_duration_minutes INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lecon (
                        id INT NOT NULL AUTO_INCREMENT,
                        titre VARCHAR(255) NOT NULL,
                        ordre INT NOT NULL,
                        media_type VARCHAR(255) NOT NULL,
                        media_url VARCHAR(255) NOT NULL,
                        cours_id INT DEFAULT NULL,
                        video_url VARCHAR(255) DEFAULT NULL,
                        youtube_url VARCHAR(255) DEFAULT NULL,
                        image VARCHAR(255) DEFAULT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                        duration_minutes INT NOT NULL DEFAULT 10,
                        PRIMARY KEY (id),
                        KEY idx_lecon_cours (cours_id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user (
                        id INT NOT NULL AUTO_INCREMENT,
                        email VARCHAR(180) NOT NULL,
                        roles LONGTEXT NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        first_name VARCHAR(255) DEFAULT NULL,
                        last_name VARCHAR(255) DEFAULT NULL,
                        is_active TINYINT(1) NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE KEY uniq_user_email (email)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_cours_progress (
                        id INT NOT NULL AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        cours_id INT NOT NULL,
                        progress INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (id),
                        UNIQUE KEY uniq_user_course (user_id, cours_id),
                        KEY idx_progress_user (user_id),
                        KEY idx_progress_course (cours_id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_lecon_progress (
                        id INT NOT NULL AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        lesson_id INT NOT NULL,
                        completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (id),
                        UNIQUE KEY uniq_user_lesson (user_id, lesson_id),
                        KEY idx_lesson_progress_user (user_id),
                        KEY idx_lesson_progress_lesson (lesson_id)
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'initialiser les tables EduKids.", exception);
        }

        try {
            boolean courseStatusAdded = addColumnIfMissing(cnx, "cours", "status", "VARCHAR(20) NOT NULL DEFAULT 'DRAFT'");
            addColumnIfMissing(cnx, "cours", "lesson_count", "INT NOT NULL DEFAULT 0");
            addColumnIfMissing(cnx, "cours", "total_duration_minutes", "INT NOT NULL DEFAULT 0");

            boolean lessonStatusAdded = addColumnIfMissing(cnx, "lecon", "status", "VARCHAR(20) NOT NULL DEFAULT 'DRAFT'");
            addColumnIfMissing(cnx, "lecon", "duration_minutes", "INT NOT NULL DEFAULT 10");

            if (courseStatusAdded) {
                backfillLegacyCourseStatus(cnx);
            }
            if (lessonStatusAdded) {
                updateAllRows(cnx, "UPDATE lecon SET status = 'PUBLISHED'");
            }

            normalizeCourseStatuses(cnx);
            normalizeLessonStatuses(cnx);
            normalizeLessonOrdering(cnx);
            refreshCourseStats(cnx);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'initialiser les colonnes EduKids.", exception);
        }
    }

    private static boolean addColumnIfMissing(Connection cnx, String tableName, String columnName, String definition) throws SQLException {
        if (columnExists(cnx, tableName, columnName)) {
            return false;
        }

        try (Statement statement = cnx.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
        return true;
    }

    private static boolean columnExists(Connection cnx, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = cnx.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(cnx.getCatalog(), null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private static void backfillLegacyCourseStatus(Connection cnx) throws SQLException {
        updateAllRows(cnx, """
                UPDATE cours c
                SET status = CASE
                    WHEN EXISTS (SELECT 1 FROM lecon l WHERE l.cours_id = c.id) THEN 'PUBLISHED'
                    ELSE 'DRAFT'
                END
                """);
    }

    private static void normalizeCourseStatuses(Connection cnx) throws SQLException {
        updateAllRows(cnx, "UPDATE cours SET status = UPPER(TRIM(COALESCE(status, 'DRAFT')))");
        updateAllRows(cnx, "UPDATE cours SET status = 'DRAFT' WHERE status NOT IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')");
    }

    private static void normalizeLessonStatuses(Connection cnx) throws SQLException {
        updateAllRows(cnx, "UPDATE lecon SET status = UPPER(TRIM(COALESCE(status, 'DRAFT')))");
        updateAllRows(cnx, "UPDATE lecon SET status = 'DRAFT' WHERE status NOT IN ('DRAFT', 'PUBLISHED', 'HIDDEN')");
    }

    private static void normalizeLessonOrdering(Connection cnx) throws SQLException {
        String selectSql = """
                SELECT id, cours_id
                FROM lecon
                WHERE cours_id IS NOT NULL
                ORDER BY cours_id ASC, ordre ASC, id ASC
                """;
        String updateSql = "UPDATE lecon SET ordre = ? WHERE id = ?";

        try (PreparedStatement selectStatement = cnx.prepareStatement(selectSql);
             PreparedStatement updateStatement = cnx.prepareStatement(updateSql);
             ResultSet resultSet = selectStatement.executeQuery()) {
            long currentCourseId = -1;
            int expectedOrder = 0;

            while (resultSet.next()) {
                long courseId = resultSet.getLong("cours_id");
                if (courseId != currentCourseId) {
                    currentCourseId = courseId;
                    expectedOrder = 1;
                } else {
                    expectedOrder++;
                }

                updateStatement.setInt(1, expectedOrder);
                updateStatement.setLong(2, resultSet.getLong("id"));
                updateStatement.addBatch();
            }

            updateStatement.executeBatch();
        }
    }

    private static void refreshCourseStats(Connection cnx) throws SQLException {
        updateAllRows(cnx, """
                UPDATE cours c
                SET lesson_count = (
                        SELECT COUNT(*)
                        FROM lecon l
                        WHERE l.cours_id = c.id
                    ),
                    total_duration_minutes = (
                        SELECT COALESCE(SUM(l.duration_minutes), 0)
                        FROM lecon l
                        WHERE l.cours_id = c.id
                    )
                """);
    }

    private static void updateAllRows(Connection cnx, String sql) throws SQLException {
        try (Statement statement = cnx.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
