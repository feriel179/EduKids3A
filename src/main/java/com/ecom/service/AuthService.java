package com.ecom.service;

import com.ecom.model.User;
import com.ecom.persistence.UserRepository;
import com.ecom.validation.ValidationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class AuthService {

    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    public User login(String email, String password) throws SQLException, ValidationException {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Veuillez saisir l'adresse e-mail et le mot de passe.");
        }

        User user = userRepository.findByEmail(email.trim());
        if (user == null) {
            throw new ValidationException("Compte introuvable.");
        }

        if (!user.isActive()) {
            throw new ValidationException("Ce compte est desactive.");
        }

        if (!passwordMatches(password.trim(), user.getPasswordHash())) {
            throw new ValidationException("Mot de passe incorrect.");
        }

        return user;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return checkBcrypt(rawPassword, storedPassword);
        }

        return hash(rawPassword).equalsIgnoreCase(storedPassword);
    }

    private boolean checkBcrypt(String rawPassword, String storedPassword) {
        try {
            Class<?> bcryptClass = Class.forName("org.mindrot.jbcrypt.BCrypt");
            Object result = bcryptClass.getMethod("checkpw", String.class, String.class)
                    .invoke(null, rawPassword, storedPassword);
            return Boolean.TRUE.equals(result);
        } catch (Exception exception) {
            return false;
        }
    }

    public static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponible.", exception);
        }
    }
}
