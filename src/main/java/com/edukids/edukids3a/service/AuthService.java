package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.User;
import com.edukids.edukids3a.utils.Myconnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public User authenticate(String emailOrUsername, String motDePasse) {
        String sql = """
                SELECT id, email, roles, password, first_name, last_name, is_active
                FROM `user`
                WHERE (email = ? OR first_name = ? OR last_name = ? OR CONCAT(first_name, ' ', last_name) = ?)
                  AND password = ?
                  AND is_active = 1
                LIMIT 1
                """;

        try (Connection connection = Myconnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, emailOrUsername);
            statement.setString(2, emailOrUsername);
            statement.setString(3, emailOrUsername);
            statement.setString(4, emailOrUsername);
            statement.setString(5, motDePasse);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setEmail(rs.getString("email"));
                    user.setRoles(rs.getString("roles"));
                    user.setPassword(rs.getString("password"));
                    user.setFirstName(rs.getString("first_name"));
                    user.setLastName(rs.getString("last_name"));
                    user.setActive(rs.getBoolean("is_active"));
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de l'authentification JDBC.", e);
        }

        return null;
    }
}
