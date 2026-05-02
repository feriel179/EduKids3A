package com.ecom.persistence;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PromoCodeRepository {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_RANDOM_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    public PromoApplication findByCodeForUpdate(Connection connection, String code) throws SQLException {
        String sql = """
                SELECT id, code, discount_percent, is_used
                FROM promo_code
                WHERE UPPER(code) = UPPER(?)
                FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new PromoApplication(
                        resultSet.getInt("id"),
                        resultSet.getString("code"),
                        resultSet.getDouble("discount_percent"),
                        resultSet.getBoolean("is_used")
                );
            }
        }
    }

    public void markAsUsed(Connection connection, int promoId, int commandeId) throws SQLException {
        String sql = """
                UPDATE promo_code
                SET is_used = TRUE, used_at = CURRENT_TIMESTAMP, used_in_commande_id = ?
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, commandeId);
            statement.setInt(2, promoId);
            statement.executeUpdate();
        }
    }

    public String createPromoCode(Connection connection, int generatedFromCommandeId, double discountPercent)
            throws SQLException {
        String sql = """
                INSERT INTO promo_code (code, discount_percent, generated_from_commande_id, is_used)
                VALUES (?, ?, ?, FALSE)
                """;

        for (int attempt = 0; attempt < 12; attempt++) {
            String candidateCode = generateCode();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, candidateCode);
                statement.setDouble(2, discountPercent);
                statement.setInt(3, generatedFromCommandeId);
                statement.executeUpdate();
                return candidateCode;
            } catch (SQLException exception) {
                if (isDuplicateCodeError(exception)) {
                    continue;
                }
                throw exception;
            }
        }

        throw new SQLException("Impossible de generer un code promo unique.");
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder("EDU-");
        for (int i = 0; i < CODE_RANDOM_LENGTH; i++) {
            int index = RANDOM.nextInt(CODE_ALPHABET.length());
            builder.append(CODE_ALPHABET.charAt(index));
        }
        return builder.toString();
    }

    private boolean isDuplicateCodeError(SQLException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        return "23000".equals(exception.getSQLState()) || message.contains("duplicate");
    }

    public record PromoApplication(int id, String code, double discountPercent, boolean used) {
    }
}
