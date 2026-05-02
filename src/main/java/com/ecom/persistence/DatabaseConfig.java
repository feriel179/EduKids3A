package com.ecom.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseConfig {

    static {
        initializeSchema();
    }

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                com.edukids.edukids3a.utils.DatabaseConfig.databaseUrl(),
                jdbcProperties()
        );
    }

    private static Properties jdbcProperties() {
        Properties properties = new Properties();
        properties.putAll(com.edukids.edukids3a.utils.DatabaseConfig.jdbcProperties());
        return properties;
    }

    private static void initializeSchema() {
        String createCategoryTable = """
                CREATE TABLE IF NOT EXISTS category (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    nom VARCHAR(100) NOT NULL,
                    description VARCHAR(255)
                )
                """;
        String createProduitTable = """
                CREATE TABLE IF NOT EXISTS produit (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    nom VARCHAR(150) NOT NULL,
                    description TEXT NOT NULL,
                    image_url VARCHAR(500) NOT NULL,
                    prix DOUBLE NOT NULL,
                    stock INT NOT NULL,
                    category_id INT NOT NULL,
                    CONSTRAINT fk_produit_category
                        FOREIGN KEY (category_id) REFERENCES category(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
                """;
        String createCommandeTable = """
                CREATE TABLE IF NOT EXISTS commande (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    produit_id INT NOT NULL,
                    quantite INT NOT NULL,
                    montant_initial DOUBLE NULL,
                    remise DOUBLE NOT NULL DEFAULT 0.0,
                    montant_total DOUBLE NOT NULL,
                    code_promo_utilise VARCHAR(30) NULL,
                    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
                    commentaire VARCHAR(255),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_commande_produit
                        FOREIGN KEY (produit_id) REFERENCES produit(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT
                )
                """;
        String createPromoCodeTable = """
                CREATE TABLE IF NOT EXISTS promo_code (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    code VARCHAR(30) NOT NULL UNIQUE,
                    discount_percent DOUBLE NOT NULL,
                    generated_from_commande_id INT NOT NULL,
                    used_in_commande_id INT NULL,
                    is_used BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    used_at TIMESTAMP NULL,
                    CONSTRAINT fk_promo_generated_commande
                        FOREIGN KEY (generated_from_commande_id) REFERENCES commande(id)
                        ON UPDATE CASCADE
                        ON DELETE RESTRICT,
                    CONSTRAINT fk_promo_used_commande
                        FOREIGN KEY (used_in_commande_id) REFERENCES commande(id)
                        ON UPDATE CASCADE
                        ON DELETE SET NULL
                )
                """;

        try (Connection connection = DriverManager.getConnection(
                com.edukids.edukids3a.utils.DatabaseConfig.databaseUrl(),
                jdbcProperties());
             PreparedStatement categoryStatement = connection.prepareStatement(createCategoryTable);
             PreparedStatement produitStatement = connection.prepareStatement(createProduitTable);
             PreparedStatement commandeStatement = connection.prepareStatement(createCommandeTable);
             PreparedStatement promoCodeStatement = connection.prepareStatement(createPromoCodeTable)) {
            categoryStatement.executeUpdate();
            produitStatement.executeUpdate();
            commandeStatement.executeUpdate();
            promoCodeStatement.executeUpdate();
            seedDefaultData(connection);
            removeUserColumnIfPresent(connection);
            removePromoUserColumnIfPresent(connection);
            ensureCommandePricingColumns(connection);
            ensurePromoCodeColumns(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'initialiser les tables commande et promo_code.", exception);
        }
    }

    private static void seedDefaultData(Connection connection) throws SQLException {
        insertCategoryIfMissing(connection, "Informatique", "Ordinateurs, accessoires et composants");
        insertCategoryIfMissing(connection, "Maison", "Decoration et equipements");
        insertCategoryIfMissing(connection, "Sport", "Produits pour l'activite physique");
    }

    private static void insertCategoryIfMissing(Connection connection, String name, String description) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO category (nom, description)
                SELECT ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM category WHERE nom = ?)
                """)) {
            statement.setString(1, name);
            statement.setString(2, description);
            statement.setString(3, name);
            statement.executeUpdate();
        }
    }

    private static void removeUserColumnIfPresent(Connection connection) throws SQLException {
        if (!columnExists(connection, "commande", "user_id")) {
            return;
        }

        dropForeignKeysForColumn(connection, "commande", "user_id");
        try (Statement alterStatement = connection.createStatement()) {
            alterStatement.executeUpdate("ALTER TABLE commande DROP COLUMN user_id");
        }
    }

    private static void removePromoUserColumnIfPresent(Connection connection) throws SQLException {
        if (!columnExists(connection, "promo_code", "user_id")) {
            return;
        }

        dropForeignKeysForColumn(connection, "promo_code", "user_id");
        try (Statement alterStatement = connection.createStatement()) {
            alterStatement.executeUpdate("ALTER TABLE promo_code DROP COLUMN user_id");
        }
    }

    private static void dropForeignKeysForColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT CONSTRAINT_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """);
             Statement alterStatement = connection.createStatement()) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String constraintName = resultSet.getString("CONSTRAINT_NAME");
                    if (constraintName != null && !constraintName.isBlank()) {
                        alterStatement.executeUpdate(
                                "ALTER TABLE `" + escapeIdentifier(tableName) + "` DROP FOREIGN KEY `" + escapeIdentifier(constraintName) + "`"
                        );
                    }
                }
            }
        }
    }

    private static void addColumnIfMissing(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }

        try (Statement alterStatement = connection.createStatement()) {
            alterStatement.executeUpdate("ALTER TABLE `" + escapeIdentifier(tableName) + "` ADD COLUMN " + definition);
        }
    }

    private static void ensureCommandePricingColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "commande", "montant_initial", "montant_initial DOUBLE NULL");
        addColumnIfMissing(connection, "commande", "remise", "remise DOUBLE NOT NULL DEFAULT 0.0");
        addColumnIfMissing(connection, "commande", "code_promo_utilise", "code_promo_utilise VARCHAR(30) NULL");
    }

    private static void ensurePromoCodeColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "promo_code", "discount_percent", "discount_percent DOUBLE NOT NULL DEFAULT 10.0");
        addColumnIfMissing(connection, "promo_code", "generated_from_commande_id", "generated_from_commande_id INT NULL");
        addColumnIfMissing(connection, "promo_code", "used_in_commande_id", "used_in_commande_id INT NULL");
        addColumnIfMissing(connection, "promo_code", "is_used", "is_used BOOLEAN NOT NULL DEFAULT FALSE");
        addColumnIfMissing(connection, "promo_code", "created_at", "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        addColumnIfMissing(connection, "promo_code", "used_at", "used_at TIMESTAMP NULL");
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                LIMIT 1
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String escapeIdentifier(String identifier) {
        return identifier.replace("`", "``");
    }
}
