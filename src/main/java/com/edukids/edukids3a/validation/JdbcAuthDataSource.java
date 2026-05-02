package com.edukids.edukids3a.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Connexion JDBC dédiée à l’authentification (table {@code user}).
 * Par défaut : même base que {@code persistence.xml} ({@code edukidsj}).
 * Surcharge possible via {@code /config/auth-jdbc.properties}.
 */
public final class JdbcAuthDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcAuthDataSource.class);

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/edukidsj?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                    + "&characterEncoding=UTF-8&connectTimeout=8000&socketTimeout=45000";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static Connection connection;

    private JdbcAuthDataSource() {
    }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Properties cfg = chargerConfig();
            String url = cfg.getProperty("jdbc.url", DEFAULT_URL);
            String user = cfg.getProperty("jdbc.user", DEFAULT_USER);
            String password = cfg.getProperty("jdbc.password", DEFAULT_PASSWORD);
            connection = DriverManager.getConnection(url, user, password);
            LOG.debug("JDBC auth connecté à {}", url.replaceAll("password=[^&]*", "password=***"));
        }
        return connection;
    }

    private static Properties chargerConfig() {
        Properties p = new Properties();
        try (InputStream in = JdbcAuthDataSource.class.getResourceAsStream("/config/auth-jdbc.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            LOG.warn("Lecture auth-jdbc.properties : {}", e.getMessage());
        }
        return p;
    }
}
