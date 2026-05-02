package com.ecom.persistence;

import com.ecom.model.Category;
import com.ecom.model.Commande;
import com.ecom.model.CommandeStatut;
import com.ecom.model.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandeRepository {

    public List<Commande> findAll() throws SQLException {
        String sql = buildBaseQuery() + " ORDER BY co.created_at DESC, co.id DESC";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapCommandes(resultSet);
        }
    }

    public void save(Produit produit, int quantite, String commentaire) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            double montantTotal = produit.getPrix() * quantite;
            saveWithPricing(connection, produit, quantite, commentaire, montantTotal, 0.0, montantTotal, null);
        }
    }

    public int saveWithTotal(Connection connection, Produit produit, int quantite, String commentaire, double montantTotal)
            throws SQLException {
        return saveWithPricing(connection, produit, quantite, commentaire, montantTotal, 0.0, montantTotal, null);
    }

    public int saveWithPricing(Connection connection, Produit produit, int quantite, String commentaire,
                               double montantInitial, double remise, double montantTotal, String codePromoUtilise)
            throws SQLException {
        String sql = """
                INSERT INTO commande (produit_id, quantite, montant_initial, remise, montant_total, code_promo_utilise, statut, commentaire)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, produit.getId());
            statement.setInt(2, quantite);
            statement.setDouble(3, montantInitial);
            statement.setDouble(4, remise);
            statement.setDouble(5, montantTotal);
            statement.setString(6, codePromoUtilise == null || codePromoUtilise.isBlank() ? null : codePromoUtilise.trim());
            statement.setString(7, CommandeStatut.EN_ATTENTE.name());
            statement.setString(8, commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        throw new SQLException("Impossible de recuperer l'ID de la commande.");
    }

    public void updateStatus(int commandeId, CommandeStatut statut) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            try {
                OrderData orderData = findOrderDataForUpdate(connection, commandeId);

                if (orderData == null) {
                    throw new SQLException("Commande introuvable.");
                }
                if (orderData.statut != CommandeStatut.EN_ATTENTE) {
                    throw new SQLException("Cette commande a deja ete traitee.");
                }

                if (statut == CommandeStatut.VALIDEE) {
                    updateStockForValidation(connection, orderData);
                }

                try (PreparedStatement updateCommande = connection.prepareStatement(
                        "UPDATE commande SET statut = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    updateCommande.setString(1, statut.name());
                    updateCommande.setInt(2, commandeId);
                    updateCommande.executeUpdate();
                }

                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private OrderData findOrderDataForUpdate(Connection connection, int commandeId) throws SQLException {
        String sql = """
                SELECT co.id, co.quantite, co.statut, p.id AS produit_id, p.stock
                FROM commande co
                JOIN produit p ON p.id = co.produit_id
                WHERE co.id = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, commandeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new OrderData(
                        resultSet.getInt("id"),
                        resultSet.getInt("produit_id"),
                        resultSet.getInt("quantite"),
                        resultSet.getInt("stock"),
                        CommandeStatut.valueOf(resultSet.getString("statut"))
                );
            }
        }
    }

    private void updateStockForValidation(Connection connection, OrderData orderData) throws SQLException {
        if (orderData.quantite > orderData.stock) {
            throw new SQLException("Stock insuffisant pour valider cette commande.");
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE produit SET stock = stock - ? WHERE id = ?")) {
            statement.setInt(1, orderData.quantite);
            statement.setInt(2, orderData.produitId);
            statement.executeUpdate();
        }
    }

    private String buildBaseQuery() {
        return """
                SELECT co.id, co.quantite,
                       COALESCE(co.montant_initial, p.prix * co.quantite) AS montant_initial,
                       COALESCE(co.remise, GREATEST(COALESCE(co.montant_initial, p.prix * co.quantite) - co.montant_total, 0)) AS remise,
                       co.montant_total, co.code_promo_utilise, co.statut, co.commentaire, co.created_at,
                       p.id AS produit_id, p.nom AS produit_nom, p.description AS produit_description,
                       p.image_url, p.prix, p.stock,
                       c.id AS category_id, c.nom AS category_nom, c.description AS category_description
                FROM commande co
                JOIN produit p ON co.produit_id = p.id
                JOIN category c ON p.category_id = c.id
                """;
    }

    private List<Commande> mapCommandes(ResultSet resultSet) throws SQLException {
        List<Commande> commandes = new ArrayList<>();

        while (resultSet.next()) {
            Category category = new Category(
                    resultSet.getInt("category_id"),
                    resultSet.getString("category_nom"),
                    resultSet.getString("category_description")
            );

            Produit produit = new Produit(
                    resultSet.getInt("produit_id"),
                    resultSet.getString("produit_nom"),
                    resultSet.getString("produit_description"),
                    resultSet.getString("image_url"),
                    resultSet.getDouble("prix"),
                    resultSet.getInt("stock"),
                    category
            );

            Timestamp createdAt = resultSet.getTimestamp("created_at");
            commandes.add(new Commande(
                    resultSet.getInt("id"),
                    produit,
                    "Client direct",
                    resultSet.getInt("quantite"),
                    resultSet.getDouble("montant_initial"),
                    resultSet.getDouble("remise"),
                    resultSet.getDouble("montant_total"),
                    resultSet.getString("code_promo_utilise"),
                    CommandeStatut.valueOf(resultSet.getString("statut")),
                    resultSet.getString("commentaire"),
                    createdAt == null ? null : createdAt.toLocalDateTime()
            ));
        }

        return commandes;
    }

    private record OrderData(int commandeId, int produitId, int quantite, int stock, CommandeStatut statut) {
    }
}
