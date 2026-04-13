package com.edukids.edukids3a.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
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

        try {
            Files.createDirectories(DATABASE_PATH.getParent());
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS quiz (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            titre TEXT NOT NULL,
                            description TEXT NOT NULL,
                            niveau TEXT NOT NULL,
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
            }
            initialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser la base de donnees.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(JDBC_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }
}
