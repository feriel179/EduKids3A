package com.edukids.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/edukids";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyConnection instance;
    private Connection cnx;

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully.");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException("Cannot connect to database", e);
        }
    }

    public static synchronized MyConnection getInstance() {
        if (instance == null || instance.isConnectionClosed()) {
            instance = new MyConnection();
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
