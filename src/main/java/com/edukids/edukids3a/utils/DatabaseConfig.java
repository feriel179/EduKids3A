package com.edukids.edukids3a.utils;

import tn.esprit.util.AppSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class DatabaseConfig {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3308;
    public static final String DEFAULT_DATABASE = "edukids";
    public static final String DEFAULT_USER = "root";
    public static final String DEFAULT_PASSWORD = "";

    private DatabaseConfig() {
    }

    public static String baseUrl() {
        return "jdbc:mysql://" + host() + ":" + port();
    }

    public static String databaseUrl() {
        return baseUrl() + "/" + databaseName();
    }

    public static String databaseName() {
        return AppSettings.get("EDUKIDS_DB_NAME", DEFAULT_DATABASE);
    }

    public static String host() {
        return AppSettings.get("EDUKIDS_DB_HOST", DEFAULT_HOST);
    }

    public static int port() {
        String value = AppSettings.get("EDUKIDS_DB_PORT", String.valueOf(DEFAULT_PORT));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return DEFAULT_PORT;
        }
    }

    public static String user() {
        return AppSettings.get("EDUKIDS_DB_USER", DEFAULT_USER);
    }

    public static String password() {
        return AppSettings.get("EDUKIDS_DB_PASSWORD", DEFAULT_PASSWORD);
    }

    public static Properties jdbcProperties() {
        Properties props = new Properties();
        props.setProperty("user", user());
        props.setProperty("password", password());
        props.setProperty("useSSL", "false");
        props.setProperty("allowPublicKeyRetrieval", "true");
        props.setProperty("serverTimezone", "UTC");
        props.setProperty("characterEncoding", "UTF-8");
        props.setProperty("connectTimeout", "8000");
        props.setProperty("socketTimeout", "45000");
        props.setProperty("autoReconnect", "true");
        props.setProperty("maxReconnects", "3");
        return props;
    }

    public static Map<String, Object> jpaOverrides() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver");
        properties.put("jakarta.persistence.jdbc.url",
                databaseUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                        + "&characterEncoding=UTF-8&connectTimeout=8000&socketTimeout=45000"
                        + "&autoReconnect=true&maxReconnects=3");
        properties.put("jakarta.persistence.jdbc.user", user());
        properties.put("jakarta.persistence.jdbc.password", password());
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.jdbc.time_zone", "UTC");
        properties.put("hibernate.connection.autocommit", "true");
        properties.put("hibernate.connection.pool_size", "10");
        return properties;
    }
}
