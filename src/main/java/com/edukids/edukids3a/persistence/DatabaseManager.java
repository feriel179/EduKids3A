package com.edukids.edukids3a.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private static final Path DATABASE_PATH = Paths.get("data", "edukids.db");
    private static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_PATH.toAbsolutePath();
    private static boolean initialized;

    private DatabaseManager() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        Connection connection = null;
        try {
            Files.createDirectories(DATABASE_PATH.getParent());
            connection = getConnection();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS quiz (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            titre TEXT NOT NULL,
                            description TEXT NOT NULL,
                            image_url TEXT,
                            niveau TEXT NOT NULL,
                            categorie_age TEXT NOT NULL DEFAULT '10 ans et plus',
                            nombre_questions INTEGER NOT NULL,
                            duree_minutes INTEGER NOT NULL,
                            score_minimum INTEGER NOT NULL,
                            statut TEXT NOT NULL
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS question (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            quiz_id INTEGER NOT NULL,
                            intitule TEXT NOT NULL,
                            type TEXT NOT NULL,
                            points INTEGER NOT NULL,
                            FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS reponse (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            question_id INTEGER NOT NULL,
                            texte TEXT NOT NULL,
                            correcte INTEGER NOT NULL,
                            FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
                        )
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_question_quiz_id
                        ON question(quiz_id)
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_reponse_question_id
                        ON reponse(question_id)
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS quiz_result (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            quiz_id INTEGER NOT NULL,
                            final_score INTEGER NOT NULL,
                            earned_points INTEGER NOT NULL,
                            total_points INTEGER NOT NULL,
                            completed_at TEXT NOT NULL,
                            FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
                        )
                        """);
                statement.executeUpdate("""
                        CREATE INDEX IF NOT EXISTS idx_quiz_result_quiz_id
                        ON quiz_result(quiz_id)
                        """);
                ensureColumnExists(connection, "quiz", "image_url", "TEXT");
                ensureColumnExists(connection, "quiz", "categorie_age", "TEXT NOT NULL DEFAULT '10 ans et plus'");
            }
            repairData(connection);
            connection.commit();
            initialized = true;
        } catch (Exception e) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Impossible d'initialiser la base de donnees.", e);
        } finally {
            closeQuietly(connection);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(JDBC_URL);
        configureConnection(connection);
        return connection;
    }

    private static void configureConnection(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
    }

    private static void ensureColumnExists(Connection connection, String tableName, String columnName, String columnDefinition) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("PRAGMA table_info(" + tableName + ")");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void repairData(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    DELETE FROM reponse
                    WHERE question_id NOT IN (SELECT id FROM question)
                    """);
            statement.executeUpdate("""
                    DELETE FROM question
                    WHERE quiz_id NOT IN (SELECT id FROM quiz)
                    """);
            statement.executeUpdate("""
                    DELETE FROM quiz_result
                    WHERE quiz_id NOT IN (SELECT id FROM quiz)
                    """);
            statement.executeUpdate("""
                    UPDATE quiz
                    SET categorie_age = '10 ans et plus'
                    WHERE categorie_age IS NULL OR TRIM(categorie_age) = ''
                    """);
            statement.executeUpdate("""
                    UPDATE quiz
                    SET image_url = ''
                    WHERE image_url IS NULL
                    """);
            statement.executeUpdate("""
                    UPDATE quiz
                    SET nombre_questions = (
                        SELECT COUNT(*)
                        FROM question
                        WHERE question.quiz_id = quiz.id
                    )
                    """);
        }
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
