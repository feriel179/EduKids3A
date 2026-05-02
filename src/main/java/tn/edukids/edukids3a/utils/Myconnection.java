package com.edukids.edukids3a.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Myconnection {

    private static Myconnection instance;
    private Connection cnx;

    private Myconnection() {
        try {
            // Charger explicitement le driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL driver loaded successfully.");
            
            // Créer les propriétés de connexion
            Properties props = DatabaseConfig.jdbcProperties();
            
            // D'abord, créer la base de données si elle n'existe pas
            createDatabaseIfNotExists(props);
            
            // Ensuite, se connecter à la base de données
            cnx = DriverManager.getConnection(DatabaseConfig.databaseUrl(), props);
            System.out.println("Database connected successfully to: " + DatabaseConfig.databaseUrl());
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver not found: " + e.getMessage());
            throw new RuntimeException("MySQL driver not found", e);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot connect to database", e);
        }
    }

    private static void createDatabaseIfNotExists(Properties props) {
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.baseUrl(), props);
             Statement stmt = conn.createStatement()) {
            
            String sql = "CREATE DATABASE IF NOT EXISTS " + DatabaseConfig.databaseName() +
                         " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            stmt.executeUpdate(sql);
            System.out.println("Database '" + DatabaseConfig.databaseName() + "' ensured to exist.");
            
        } catch (SQLException e) {
            System.err.println("Failed to create database: " + e.getMessage());
            // Ne pas arrêter si la création échoue, la base existe peut-être déjà
        }
    }

    public static synchronized Myconnection getInstance() {
        if (instance == null || instance.isConnectionClosed()) {
            instance = new Myconnection();
        }
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }

    private boolean isConnectionClosed() {
        try {
            return cnx == null || cnx.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }
}
