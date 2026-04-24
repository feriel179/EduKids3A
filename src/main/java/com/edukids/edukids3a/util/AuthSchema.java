package com.edukids.edukids3a.util;

import com.edukids.edukids3a.models.Role;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Crée la table {@code user} (modèle Rami) et garantit des comptes de test si absents.
 */
public final class AuthSchema {

    private static final Logger LOG = LoggerFactory.getLogger(AuthSchema.class);

    /** Mot de passe commun pour tous les comptes de démonstration (à changer en production). */
    public static final String DEMO_PASSWORD = "edukids";

    private AuthSchema() {
    }

    public static void ensureUserTableAndSeed(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS `user` (
                      `id` INT NOT NULL AUTO_INCREMENT,
                      `email` VARCHAR(255) NOT NULL,
                      `roles` TEXT,
                      `password` VARCHAR(255) NOT NULL,
                      `first_name` VARCHAR(120) DEFAULT NULL,
                      `last_name` VARCHAR(120) DEFAULT NULL,
                      `is_active` TINYINT(1) NOT NULL DEFAULT 1,
                      `avatar` VARCHAR(500) DEFAULT NULL,
                      `is_verified` TINYINT(1) NOT NULL DEFAULT 0,
                      PRIMARY KEY (`id`),
                      UNIQUE KEY `uk_user_email` (`email`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        }

        ensureDemoUser(c, "admin@edukids.local", List.of(Role.ROLE_ADMIN), DEMO_PASSWORD, "Admin", "EduKids");
        ensureDemoUser(c, "parent@edukids.local", List.of(Role.ROLE_PARENT), DEMO_PASSWORD, "Marie", "Martin");
    }

    /**
     * Insère l’utilisateur si aucune ligne n’existe pour cet e-mail (idempotent au redémarrage).
     */
    private static void ensureDemoUser(Connection c, String email, List<Role> roles, String plainPassword,
                                       String firstName, String lastName) throws SQLException {
        String check = "SELECT 1 FROM `user` WHERE email=?";
        try (PreparedStatement ps = c.prepareStatement(check)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        String rolesJson = new Gson().toJson(roles.stream().map(Role::getDbValue).toList());
        String sql = """
                INSERT INTO `user` (email, roles, password, first_name, last_name, is_active, avatar, is_verified)
                VALUES (?, ?, ?, ?, ?, 1, NULL, 1)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, rolesJson);
            ps.setString(3, hash);
            ps.setString(4, firstName);
            ps.setString(5, lastName);
            ps.executeUpdate();
        }
        String roleLabel = roles.isEmpty() ? "" : roles.get(0).getDisplayName();
        LOG.info("Compte de test créé : {} ({}) — mot de passe : {}", email, roleLabel, plainPassword);
    }
}
