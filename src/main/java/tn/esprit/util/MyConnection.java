package tn.esprit.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MyConnection {
    private static MyConnection instance;

    private final String user;
    private final String password;
    private Connection cnx;

    private MyConnection() {
        user = readSetting("EDUKIDS_DB_USER", "root");
        password = readSetting("EDUKIDS_DB_PASSWORD", "");
        cnx = openConnection();
        DatabaseInitializer.initialize(cnx);
    }

    public static synchronized MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public synchronized Connection getCnx() {
        try {
            if (cnx == null || cnx.isClosed() || !cnx.isValid(2)) {
                cnx = openConnection();
                DatabaseInitializer.initialize(cnx);
            }
            return cnx;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de reutiliser la connexion EduKids.", exception);
        }
    }

    private Connection openConnection() {
        SQLException lastException = null;
        for (DatabaseCandidate candidate : buildCandidates()) {
            try {
                return DriverManager.getConnection(candidate.databaseUrl(), user, password);
            } catch (SQLException exception) {
                lastException = exception;
            }
        }

        for (ServerCandidate server : buildServers()) {
            try (Connection serverConnection = DriverManager.getConnection(server.serverUrl(), user, password);
                 Statement statement = serverConnection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `edukids` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                return DriverManager.getConnection(server.databaseUrl("edukids"), user, password);
            } catch (SQLException exception) {
                lastException = exception;
            }
        }

        throw new IllegalStateException(
                "Impossible de se connecter a la base EduKids. Verifie MySQL/MariaDB sur 127.0.0.1:3306 ou localhost:5599.",
                lastException
        );
    }

    private List<DatabaseCandidate> buildCandidates() {
        List<DatabaseCandidate> candidates = new ArrayList<>();
        for (String host : buildHosts()) {
            for (int port : buildPorts()) {
                for (String databaseName : buildDatabaseNames()) {
                    candidates.add(new DatabaseCandidate(host, port, databaseName));
                }
            }
        }
        return candidates;
    }

    private List<ServerCandidate> buildServers() {
        List<ServerCandidate> servers = new ArrayList<>();
        for (String host : buildHosts()) {
            for (int port : buildPorts()) {
                servers.add(new ServerCandidate(host, port));
            }
        }
        return servers;
    }

    private List<String> buildHosts() {
        Set<String> hosts = new LinkedHashSet<>();
        hosts.add(readSetting("EDUKIDS_DB_HOST", "127.0.0.1"));
        hosts.add("localhost");
        return new ArrayList<>(hosts);
    }

    private List<Integer> buildPorts() {
        Set<Integer> ports = new LinkedHashSet<>();
        ports.add(readIntSetting("EDUKIDS_DB_PORT", 3306));
        ports.add(5599);
        return new ArrayList<>(ports);
    }

    private List<String> buildDatabaseNames() {
        Set<String> databaseNames = new LinkedHashSet<>();
        databaseNames.add(readSetting("EDUKIDS_DB_NAME", "edukids"));
        databaseNames.add("EduKids");
        return new ArrayList<>(databaseNames);
    }

    private String readSetting(String key, String fallback) {
        return AppSettings.get(key, fallback);
    }

    private int readIntSetting(String key, int fallback) {
        String value = readSetting(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record DatabaseCandidate(String host, int port, String databaseName) {
        private String databaseUrl() {
            return "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
    }

    private record ServerCandidate(String host, int port) {
        private String serverUrl() {
            return "jdbc:mysql://" + host + ":" + port
                    + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }

        private String databaseUrl(String databaseName) {
            return "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
    }
}
