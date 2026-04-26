package com.edukids.edukids3a.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Initialise automatiquement la base de données et ses tables au démarrage.
 */
public class DatabaseInitializer {

    private static final String BASE_URL = "jdbc:mysql://localhost:3308";
    private static final String DB_NAME = "edukids";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static void initializeDatabase() {
        try {
            // Charger le driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Créer les propriétés
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("serverTimezone", "UTC");
            props.setProperty("connectTimeout", "8000");
            
            // Créer la base de données
            createDatabase(props);
            
            // Créer les tables
            createTables(props);
            
            System.out.println("✓ Database initialization completed successfully.");
            
        } catch (ClassNotFoundException e) {
            System.err.println("✗ MySQL driver not found: " + e.getMessage());
            throw new RuntimeException("MySQL driver not found", e);
        } catch (SQLException e) {
            System.err.println("✗ Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void createDatabase(Properties props) throws SQLException {
        try (Connection conn = DriverManager.getConnection(BASE_URL, props);
             Statement stmt = conn.createStatement()) {
            
            String sql = "CREATE DATABASE IF NOT EXISTS " + DB_NAME + 
                         " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            stmt.executeUpdate(sql);
            System.out.println("✓ Database '" + DB_NAME + "' ensured.");
            
        } catch (SQLException e) {
            System.err.println("✗ Failed to create database: " + e.getMessage());
            throw e;
        }
    }

    private static void createTables(Properties props) throws SQLException {
        String dbUrl = BASE_URL + "/" + DB_NAME;
        try (Connection conn = DriverManager.getConnection(dbUrl, props);
             Statement stmt = conn.createStatement()) {
            
            // Créer les tables
            createUserTable(stmt);
            createTypeEvenementTable(stmt);
            createProgrammeTable(stmt);
            createEvenementTable(stmt);
            createConversationTable(stmt);
            createConversationParticipantTable(stmt);
            createMessageTable(stmt);
            
            System.out.println("✓ All tables ensured.");
            
        } catch (SQLException e) {
            System.err.println("✗ Failed to create tables: " + e.getMessage());
            throw e;
        }
    }

    private static void createUserTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS user (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    email VARCHAR(255) NOT NULL UNIQUE," +
                "    roles VARCHAR(255) DEFAULT 'USER'," +
                "    password VARCHAR(255) NOT NULL," +
                "    first_name VARCHAR(100)," +
                "    last_name VARCHAR(100)," +
                "    is_active BOOLEAN DEFAULT TRUE," +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        stmt.executeUpdate(sql);
    }

    private static void createTypeEvenementTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS type_evenement (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    name VARCHAR(100) NOT NULL UNIQUE" +
                ")";
        stmt.executeUpdate(sql);
    }

    private static void createProgrammeTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS programme (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    name VARCHAR(255) NOT NULL," +
                "    description LONGTEXT," +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        stmt.executeUpdate(sql);
    }

    private static void createEvenementTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS evenement (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    title VARCHAR(255) NOT NULL," +
                "    description LONGTEXT," +
                "    date_start DATETIME NOT NULL," +
                "    date_end DATETIME," +
                "    location VARCHAR(255)," +
                "    type_id INT," +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    CONSTRAINT fk_evenement_type FOREIGN KEY (type_id) REFERENCES type_evenement(id) ON DELETE SET NULL" +
                ")";
        stmt.executeUpdate(sql);
    }

    private static void createConversationTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS conversation (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    created_at DATETIME(6) NOT NULL," +
                "    updated_at DATETIME(6) NOT NULL," +
                "    title VARCHAR(255) NULL," +
                "    is_group BOOLEAN NOT NULL DEFAULT FALSE," +
                "    private_key VARCHAR(255) NULL," +
                "    last_auto_reply_at DATETIME(6) NULL" +
                ")";
        stmt.executeUpdate(sql);
    }

    private static void createConversationParticipantTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS conversation_participant (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    conversation_id INT NOT NULL," +
                "    user_id INT NOT NULL," +
                "    role VARCHAR(50) NULL," +
                "    deleted_at DATETIME(6) NULL," +
                "    last_read_at DATETIME(6) NULL," +
                "    joined_at DATETIME(6) NOT NULL," +
                "    hidden_at DATETIME(6) NULL," +
                "    CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE," +
                "    CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE" +
                ")";
        stmt.executeUpdate(sql);
    }

    private static void createMessageTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS message (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    sender_id INT NOT NULL," +
                "    content LONGTEXT NOT NULL," +
                "    created_at DATETIME(6) NOT NULL," +
                "    is_read BOOLEAN NOT NULL DEFAULT FALSE," +
                "    conversation_id INT NOT NULL," +
                "    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES user(id) ON DELETE CASCADE," +
                "    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE" +
                ")";
        stmt.executeUpdate(sql);
    }
}

