package com.edukids.edukids3a.persistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseManager {
    private static final Path SQLITE_DATABASE_PATH = Paths.get("data", "edukids.db");
    private static final Path DATABASE_CONFIG_PATH = Paths.get("data", "database.properties");
    private static final String DEFAULT_SQLITE_URL = "jdbc:sqlite:" + SQLITE_DATABASE_PATH.toAbsolutePath();
    private static boolean initialized;

    private DatabaseManager() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        DatabaseSettings settings = loadSettings();
        Connection connection = null;
        try {
            if (settings.isSqlite()) {
                Files.createDirectories(SQLITE_DATABASE_PATH.getParent());
            }

            connection = getConnection();
            connection.setAutoCommit(false);
            createSchema(connection, settings);
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
        DatabaseSettings settings = loadSettings();
        Connection connection;
        if (settings.username().isBlank()) {
            connection = DriverManager.getConnection(settings.jdbcUrl());
        } else {
            connection = DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
        }
        configureConnection(connection, settings);
        return connection;
    }

    private static void createSchema(Connection connection, DatabaseSettings settings) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(settings.isSqlite() ? """
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
                    """ : """
                    CREATE TABLE IF NOT EXISTS quiz (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        titre VARCHAR(255) NOT NULL,
                        description TEXT NOT NULL,
                        image_url TEXT,
                        niveau VARCHAR(100) NOT NULL,
                        categorie_age VARCHAR(100) NOT NULL DEFAULT '10 ans et plus',
                        nombre_questions INT NOT NULL,
                        duree_minutes INT NOT NULL,
                        score_minimum INT NOT NULL,
                        statut VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            statement.executeUpdate(settings.isSqlite() ? """
                    CREATE TABLE IF NOT EXISTS question (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        quiz_id INTEGER NOT NULL,
                        intitule TEXT NOT NULL,
                        type TEXT NOT NULL,
                        points INTEGER NOT NULL,
                        FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
                    )
                    """ : """
                    CREATE TABLE IF NOT EXISTS question (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        quiz_id INT NOT NULL,
                        intitule TEXT NOT NULL,
                        type VARCHAR(100) NOT NULL,
                        points INT NOT NULL,
                        CONSTRAINT fk_question_quiz
                            FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            statement.executeUpdate(settings.isSqlite() ? """
                    CREATE TABLE IF NOT EXISTS reponse (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        question_id INTEGER NOT NULL,
                        texte TEXT NOT NULL,
                        correcte INTEGER NOT NULL,
                        FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
                    )
                    """ : """
                    CREATE TABLE IF NOT EXISTS reponse (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        question_id INT NOT NULL,
                        texte TEXT NOT NULL,
                        correcte TINYINT(1) NOT NULL,
                        CONSTRAINT fk_reponse_question
                            FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            statement.executeUpdate(settings.isSqlite() ? """
                    CREATE TABLE IF NOT EXISTS quiz_result (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        quiz_id INTEGER NOT NULL,
                        final_score INTEGER NOT NULL,
                        earned_points INTEGER NOT NULL,
                        total_points INTEGER NOT NULL,
                        completed_at TEXT NOT NULL,
                        FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
                    )
                    """ : """
                    CREATE TABLE IF NOT EXISTS quiz_result (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        quiz_id INT NOT NULL,
                        final_score INT NOT NULL,
                        earned_points INT NOT NULL,
                        total_points INT NOT NULL,
                        completed_at VARCHAR(100) NOT NULL,
                        CONSTRAINT fk_quiz_result_quiz
                            FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

        }

        createIndexIfMissing(connection, "question", "idx_question_quiz_id", "CREATE INDEX idx_question_quiz_id ON question(quiz_id)");
        createIndexIfMissing(connection, "reponse", "idx_reponse_question_id", "CREATE INDEX idx_reponse_question_id ON reponse(question_id)");
        createIndexIfMissing(connection, "quiz_result", "idx_quiz_result_quiz_id", "CREATE INDEX idx_quiz_result_quiz_id ON quiz_result(quiz_id)");
        ensureColumnExists(connection, "quiz", "image_url", settings.isSqlite() ? "TEXT" : "TEXT");
        ensureColumnExists(connection, "quiz", "categorie_age",
                settings.isSqlite()
                        ? "TEXT NOT NULL DEFAULT '10 ans et plus'"
                        : "VARCHAR(100) NOT NULL DEFAULT '10 ans et plus'");
    }

    private static void configureConnection(Connection connection, DatabaseSettings settings) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            if (settings.isSqlite()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            } else {
                statement.execute("SET NAMES utf8mb4");
            }
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
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metadata.getColumns(connection.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void createIndexIfMissing(Connection connection, String tableName, String indexName, String createIndexSql) throws SQLException {
        if (indexExists(connection, tableName, indexName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createIndexSql);
        }
    }

    private static boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (resultSet.next()) {
                String currentIndexName = resultSet.getString("INDEX_NAME");
                if (currentIndexName != null && indexName.equalsIgnoreCase(currentIndexName)) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metadata.getIndexInfo(connection.getCatalog(), null, tableName.toUpperCase(), false, false)) {
            while (resultSet.next()) {
                String currentIndexName = resultSet.getString("INDEX_NAME");
                if (currentIndexName != null && indexName.equalsIgnoreCase(currentIndexName)) {
                    return true;
                }
            }
        }
        return false;
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

    private static DatabaseSettings loadSettings() {
        Properties properties = new Properties();

        if (Files.exists(DATABASE_CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(DATABASE_CONFIG_PATH)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException("Impossible de lire la configuration de la base.", e);
            }
        }

        String dbType = readSetting(properties, "db.type", "DB_TYPE", "mysql");
        if ("mysql".equalsIgnoreCase(dbType) || "mariadb".equalsIgnoreCase(dbType)) {
            String defaultMySqlUrl = "jdbc:mysql://localhost:3308/edukids?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            String jdbcUrl = readSetting(properties, "db.url", "DB_URL", defaultMySqlUrl);
            String username = readSetting(properties, "db.user", "DB_USER", "root");
            String password = readSetting(properties, "db.password", "DB_PASSWORD", "");
            return new DatabaseSettings("mysql", jdbcUrl, username, password);
        }

        return new DatabaseSettings("sqlite", DEFAULT_SQLITE_URL, "", "");
    }

    private static String readSetting(Properties properties, String propertyKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String systemValue = System.getProperty(propertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue;
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

    private record DatabaseSettings(String type, String jdbcUrl, String username, String password) {
        private boolean isSqlite() {
            return "sqlite".equalsIgnoreCase(type);
        }
    }
}
