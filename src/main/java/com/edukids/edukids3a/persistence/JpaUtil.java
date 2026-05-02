package com.edukids.edukids3a.persistence;

import com.edukids.edukids3a.utils.DatabaseConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;

public final class JpaUtil {

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
        return DatabaseConfig.jpaOverrides();
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
