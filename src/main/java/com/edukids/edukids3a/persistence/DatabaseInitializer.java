package com.edukids.edukids3a.persistence;

import com.edukids.edukids3a.utils.DatabaseConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Initialise automatiquement la base de données et ses tables au démarrage.
 */
public class DatabaseInitializer {

    public static void initializeDatabase() {
        try {
            // Charger le driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Créer les propriétés
            Properties props = DatabaseConfig.jdbcProperties();
            
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
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.baseUrl(), props);
             Statement stmt = conn.createStatement()) {
            
            String sql = "CREATE DATABASE IF NOT EXISTS " + DatabaseConfig.databaseName() +
                         " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            stmt.executeUpdate(sql);
            System.out.println("Database '" + DatabaseConfig.databaseName() + "' ensured.");
            
        } catch (SQLException e) {
            System.err.println("✗ Failed to create database: " + e.getMessage());
            throw e;
        }
    }

    private static void createTables(Properties props) throws SQLException {
        String dbUrl = DatabaseConfig.databaseUrl();
        try (Connection conn = DriverManager.getConnection(dbUrl, props);
             Statement stmt = conn.createStatement()) {
            
            // Créer les tables
            createUserTable(stmt);
            createTypeEvenementTable(stmt);
            createEvenementTable(stmt);
            createProgrammeTable(stmt);
            createReservationTable(stmt);
            createUserEvenementInteractionTable(stmt);
            createConversationTable(stmt);
            createConversationParticipantTable(stmt);
            createMessageTable(stmt);
            ensureUserColumns(stmt);
            ensureEvenementLegacySchemaCompatibility(conn, stmt);
            ensureProgrammeSchemaCompatibility(conn, stmt);
            
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
                "    roles LONGTEXT NOT NULL," +
                "    password VARCHAR(255) NOT NULL," +
                "    first_name VARCHAR(100)," +
                "    last_name VARCHAR(100)," +
                "    is_active BOOLEAN DEFAULT TRUE," +
                "    avatar VARCHAR(255) DEFAULT NULL," +
                "    is_verified BOOLEAN NOT NULL DEFAULT FALSE," +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        stmt.executeUpdate(sql);
    }

    private static void ensureUserColumns(Statement stmt) throws SQLException {
        addColumnIfMissing(stmt, "user", "avatar", "VARCHAR(255) DEFAULT NULL");
        addColumnIfMissing(stmt, "user", "is_verified", "BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing(stmt, "user", "is_active", "BOOLEAN DEFAULT TRUE");
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
                "    evenement_id INT NOT NULL UNIQUE," +
                "    pause_debut TIME NULL," +
                "    pause_fin TIME NULL," +
                "    activites LONGTEXT NULL," +
                "    documents LONGTEXT NULL," +
                "    materiels LONGTEXT NULL," +
                "    CONSTRAINT fk_programme_evenement FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE" +
                ")";
        stmt.executeUpdate(sql);
        addColumnIfMissing(stmt, "programme", "evenement_id", "INT NULL UNIQUE");
        addColumnIfMissing(stmt, "programme", "pause_debut", "TIME NULL");
        addColumnIfMissing(stmt, "programme", "pause_fin", "TIME NULL");
        addColumnIfMissing(stmt, "programme", "activites", "LONGTEXT NULL");
        addColumnIfMissing(stmt, "programme", "documents_requis", "LONGTEXT NULL");
        addColumnIfMissing(stmt, "programme", "materiels_requis", "LONGTEXT NULL");
    }

    private static void ensureProgrammeSchemaCompatibility(Connection conn, Statement stmt) throws SQLException {
        makeColumnNullableIfExists(conn, stmt, "programme", "name", "VARCHAR(255)");
        makeColumnNullableIfExists(conn, stmt, "programme", "description", "LONGTEXT");
        makeColumnNullableIfExists(conn, stmt, "programme", "created_at", "TIMESTAMP");

        executeIgnoringErrors(stmt, """
                UPDATE programme p
                JOIN evenement e ON e.id = p.evenement_id
                SET p.name = e.titre
                WHERE (p.name IS NULL OR TRIM(p.name) = '')
                  AND e.titre IS NOT NULL
                  AND TRIM(e.titre) <> ''
                """);
        executeIgnoringErrors(stmt, """
                UPDATE programme
                SET documents_requis = documents
                WHERE (documents_requis IS NULL OR TRIM(documents_requis) = '')
                  AND documents IS NOT NULL
                  AND TRIM(documents) <> ''
                """);
        executeIgnoringErrors(stmt, """
                UPDATE programme
                SET materiels_requis = materiels
                WHERE (materiels_requis IS NULL OR TRIM(materiels_requis) = '')
                  AND materiels IS NOT NULL
                  AND TRIM(materiels) <> ''
                """);
    }

    private static void createEvenementTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS evenement (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    titre VARCHAR(255) NOT NULL," +
                "    description TEXT NOT NULL," +
                "    date_evenement DATE NOT NULL," +
                "    heure_debut TIME NOT NULL," +
                "    heure_fin TIME NOT NULL," +
                "    type_evenement VARCHAR(50) NULL," +
                "    image TEXT NULL," +
                "    localisation VARCHAR(500) NULL," +
                "    nb_places_disponibles INT NULL," +
                "    likes_count INT NOT NULL DEFAULT 0," +
                "    dislikes_count INT NOT NULL DEFAULT 0," +
                "    favorites_count INT NOT NULL DEFAULT 0" +
                ")";
        stmt.executeUpdate(sql);
        addColumnIfMissing(stmt, "evenement", "titre", "VARCHAR(255) NOT NULL");
        addColumnIfMissing(stmt, "evenement", "description", "TEXT NOT NULL");
        addColumnIfMissing(stmt, "evenement", "date_evenement", "DATE NOT NULL");
        addColumnIfMissing(stmt, "evenement", "heure_debut", "TIME NOT NULL");
        addColumnIfMissing(stmt, "evenement", "heure_fin", "TIME NOT NULL");
        addColumnIfMissing(stmt, "evenement", "type_evenement", "VARCHAR(50) NULL");
        addColumnIfMissing(stmt, "evenement", "image", "TEXT NULL");
        addColumnIfMissing(stmt, "evenement", "localisation", "VARCHAR(500) NULL");
        addColumnIfMissing(stmt, "evenement", "nb_places_disponibles", "INT NULL");
        addColumnIfMissing(stmt, "evenement", "likes_count", "INT NOT NULL DEFAULT 0");
        addColumnIfMissing(stmt, "evenement", "dislikes_count", "INT NOT NULL DEFAULT 0");
        addColumnIfMissing(stmt, "evenement", "favorites_count", "INT NOT NULL DEFAULT 0");
    }

    private static void ensureEvenementLegacySchemaCompatibility(Connection conn, Statement stmt) throws SQLException {
        if (columnExists(conn, "evenement", "titre")) {
            executeIgnoringErrors(stmt, """
                    UPDATE evenement
                    SET titre = title
                    WHERE (titre IS NULL OR TRIM(titre) = '')
                      AND title IS NOT NULL
                      AND TRIM(title) <> ''
                    """);
        }

        makeColumnNullableIfExists(conn, stmt, "evenement", "title", "VARCHAR(255)");
        executeIgnoringErrors(stmt, """
                UPDATE evenement
                SET title = titre
                WHERE (title IS NULL OR TRIM(title) = '')
                  AND titre IS NOT NULL
                  AND TRIM(titre) <> ''
                """);

        executeIgnoringErrors(stmt, """
                UPDATE evenement
                SET date_evenement = DATE(date_start)
                WHERE date_evenement IS NULL
                  AND date_start IS NOT NULL
                """);
        executeIgnoringErrors(stmt, """
                UPDATE evenement
                SET heure_debut = TIME(date_start)
                WHERE heure_debut IS NULL
                  AND date_start IS NOT NULL
                """);
        executeIgnoringErrors(stmt, """
                UPDATE evenement
                SET heure_fin = TIME(date_end)
                WHERE heure_fin IS NULL
                  AND date_end IS NOT NULL
                """);
        executeIgnoringErrors(stmt, """
                UPDATE evenement
                SET localisation = location
                WHERE (localisation IS NULL OR TRIM(localisation) = '')
                  AND location IS NOT NULL
                  AND TRIM(location) <> ''
                """);

        makeColumnNullableIfExists(conn, stmt, "evenement", "date_start", "DATETIME");
        makeColumnNullableIfExists(conn, stmt, "evenement", "date_end", "DATETIME");
        makeColumnNullableIfExists(conn, stmt, "evenement", "location", "VARCHAR(255)");
        makeColumnNullableIfExists(conn, stmt, "evenement", "type_id", "INT");
    }

    private static void createReservationTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS reservation (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    user_id INT NOT NULL," +
                "    id_evenement INT NOT NULL," +
                "    nom VARCHAR(255) NOT NULL," +
                "    prenom VARCHAR(255) NOT NULL," +
                "    email VARCHAR(255) NOT NULL," +
                "    telephone VARCHAR(255) NULL," +
                "    nb_adultes INT NOT NULL DEFAULT 0," +
                "    nb_enfants INT NOT NULL DEFAULT 0," +
                "    date_reservation DATETIME NOT NULL," +
                "    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE," +
                "    CONSTRAINT fk_reservation_evenement FOREIGN KEY (id_evenement) REFERENCES evenement(id) ON DELETE CASCADE" +
                ")";
        stmt.executeUpdate(sql);
    }

    private static void createUserEvenementInteractionTable(Statement stmt) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS user_evenement_interaction (" +
                "    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "    user_id INT NOT NULL," +
                "    evenement_id INT NOT NULL," +
                "    type_interaction VARCHAR(20) NOT NULL," +
                "    created_at DATETIME NOT NULL," +
                "    CONSTRAINT fk_uei_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE," +
                "    CONSTRAINT fk_uei_evenement FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE," +
                "    CONSTRAINT unique_user_evenement_type UNIQUE (user_id, evenement_id, type_interaction)" +
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

    private static void addColumnIfMissing(Statement stmt, String tableName, String columnName, String definition) {
        try {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        } catch (SQLException ignored) {
            // The column already exists or the SQL variant is unsupported.
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = conn.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(conn.getCatalog(), null, tableName, columnName)) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = metadata.getColumns(conn.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void executeIgnoringErrors(Statement stmt, String sql) {
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException ignored) {
            // Compatibility fixes are best-effort because older local schemas vary.
        }
    }

    private static void makeColumnNullableIfExists(Connection conn, Statement stmt, String tableName,
                                                   String columnName, String definition) throws SQLException {
        if (columnExists(conn, tableName, columnName)) {
            executeIgnoringErrors(stmt, "ALTER TABLE " + tableName + " MODIFY COLUMN " + columnName + " " + definition + " NULL");
        }
    }
}

