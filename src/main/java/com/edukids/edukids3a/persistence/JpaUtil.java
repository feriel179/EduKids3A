package com.edukids.edukids3a.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

public final class JpaUtil {

    private static final String HOST = "localhost";
    private static final int PORT = 3308;
    private static final String DATABASE = "edukids";
    private static volatile EntityManagerFactory emf;
    private static final Object EMF_LOCK = new Object();

    private JpaUtil() {
    }

    /**
     * Initialise l’usine JPA au premier appel (lazy). À utiliser au démarrage pour détecter tôt une base indisponible.
     */
    public static EntityManagerFactory getEntityManagerFactory() {
        EntityManagerFactory factory = emf;
        if (factory != null && factory.isOpen()) {
            return factory;
        }
        synchronized (EMF_LOCK) {
            factory = emf;
            if (factory != null && factory.isOpen()) {
                return factory;
            }
            emf = Persistence.createEntityManagerFactory("edukids", getJpaProperties());
            return emf;
        }
    }

    private static Map<String, Object> getJpaProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver");
        properties.put("jakarta.persistence.jdbc.url", "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8&connectTimeout=8000&socketTimeout=45000&autoReconnect=true&maxReconnects=3");
        properties.put("jakarta.persistence.jdbc.user", "root");
        properties.put("jakarta.persistence.jdbc.password", "");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.jdbc.time_zone", "UTC");
        properties.put("hibernate.connection.autocommit", "true");
        properties.put("hibernate.connection.pool_size", "10");
        return properties;
    }

    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static void shutdown() {
        synchronized (EMF_LOCK) {
            if (emf != null && emf.isOpen()) {
                emf.close();
            }
            emf = null;
        }
    }
}
