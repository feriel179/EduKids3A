package com.ecom.persistence;

import com.ecom.model.Category;
import com.ecom.model.Produit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProduitRepository {

    public List<Produit> findAll() throws SQLException {
        String sql = """
                SELECT p.id, p.nom, p.description, p.image_url, p.prix, p.stock,
                       c.id AS category_id, c.nom AS category_nom, c.description AS category_description
                FROM produit p
                JOIN category c ON p.category_id = c.id
                ORDER BY p.id DESC
                """;

        List<Produit> produits = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Category category = new Category(
                        resultSet.getInt("category_id"),
                        resultSet.getString("category_nom"),
                        resultSet.getString("category_description")
                );

                produits.add(new Produit(
                        resultSet.getInt("id"),
                        resultSet.getString("nom"),
                        resultSet.getString("description"),
                        resultSet.getString("image_url"),
                        resultSet.getDouble("prix"),
                        resultSet.getInt("stock"),
                        category
                ));
            }
        }

        return produits;
    }

    public void save(Produit produit) throws SQLException {
        String sql = "INSERT INTO produit (nom, description, image_url, prix, stock, category_id) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, produit.getNom());
            statement.setString(2, produit.getDescription());
            statement.setString(3, produit.getImageUrl());
            statement.setDouble(4, produit.getPrix());
            statement.setInt(5, produit.getStock());
            statement.setInt(6, produit.getCategory().getId());
            statement.executeUpdate();
        }
    }

    public void update(Produit produit) throws SQLException {
        String sql = "UPDATE produit SET nom = ?, description = ?, image_url = ?, prix = ?, stock = ?, category_id = ? WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, produit.getNom());
            statement.setString(2, produit.getDescription());
            statement.setString(3, produit.getImageUrl());
            statement.setDouble(4, produit.getPrix());
            statement.setInt(5, produit.getStock());
            statement.setInt(6, produit.getCategory().getId());
            statement.setInt(7, produit.getId());
            statement.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id = ?";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }
}
