package tn.esprit.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize(Connection cnx) {
        try (Statement statement = cnx.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS cours (
                        id INT NOT NULL AUTO_INCREMENT,
                        titre VARCHAR(255) NOT NULL,
                        description VARCHAR(255) NOT NULL,
                        niveau INT NOT NULL,
                        matiere VARCHAR(255) NOT NULL,
                        image VARCHAR(255) NOT NULL,
                        likes INT NOT NULL DEFAULT 0,
                        dislikes INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lecon (
                        id INT NOT NULL AUTO_INCREMENT,
                        titre VARCHAR(255) NOT NULL,
                        ordre INT NOT NULL,
                        media_type VARCHAR(255) NOT NULL,
                        media_url VARCHAR(255) NOT NULL,
                        cours_id INT DEFAULT NULL,
                        video_url VARCHAR(255) DEFAULT NULL,
                        youtube_url VARCHAR(255) DEFAULT NULL,
                        image VARCHAR(255) DEFAULT NULL,
                        PRIMARY KEY (id),
                        KEY idx_lecon_cours (cours_id)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user (
                        id INT NOT NULL AUTO_INCREMENT,
                        email VARCHAR(180) NOT NULL,
                        roles LONGTEXT NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        first_name VARCHAR(255) DEFAULT NULL,
                        last_name VARCHAR(255) DEFAULT NULL,
                        is_active TINYINT(1) NOT NULL,
                        PRIMARY KEY (id),
                        UNIQUE KEY uniq_user_email (email)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_cours_progress (
                        id INT NOT NULL AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        cours_id INT NOT NULL,
                        progress INT NOT NULL DEFAULT 0,
                        PRIMARY KEY (id),
                        UNIQUE KEY uniq_user_course (user_id, cours_id),
                        KEY idx_progress_user (user_id),
                        KEY idx_progress_course (cours_id)
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'initialiser les tables EduKids.", exception);
        }
    }
}
