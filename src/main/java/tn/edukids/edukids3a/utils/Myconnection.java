package com.edukids.edukids3a.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Myconnection {

    private static final String BASE_URL = "jdbc:mysql://localhost:3308";
    private static final String DB_URL = "jdbc:mysql://localhost:3308/edukids";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final String DB_NAME = "edukids";

    private static Myconnection instance;
    private Connection cnx;

    private Myconnection() {
        try {
            // Charger explicitement le driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL driver loaded successfully.");
            
            // Créer les propriétés de connexion
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("serverTimezone", "UTC");
            props.setProperty("characterEncoding", "UTF-8");
            props.setProperty("connectTimeout", "8000");
            props.setProperty("socketTimeout", "45000");
            props.setProperty("autoReconnect", "true");
            props.setProperty("maxReconnects", "3");
            
            // D'abord, créer la base de données si elle n'existe pas
            createDatabaseIfNotExists(props);
            
            // Ensuite, se connecter à la base de données
            cnx = DriverManager.getConnection(DB_URL, props);
            System.out.println("Database connected successfully to: " + DB_URL);
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
        try (Connection conn = DriverManager.getConnection(BASE_URL, props);
             Statement stmt = conn.createStatement()) {
            
            String sql = "CREATE DATABASE IF NOT EXISTS " + DB_NAME + 
                         " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            stmt.executeUpdate(sql);
            System.out.println("Database '" + DB_NAME + "' ensured to exist.");
            
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
