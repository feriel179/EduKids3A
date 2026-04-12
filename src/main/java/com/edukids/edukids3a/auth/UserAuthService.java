package com.edukids.edukids3a.auth;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Authentification et lecture utilisateur (logique alignée sur le projet Rami / JDBC). */
public final class UserAuthService {

    private final Connection connection;

    public UserAuthService(Connection connection) {
        this.connection = connection;
    }

    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public User authenticate(String email, String password) throws SQLException {
        String sql = "SELECT * FROM `user` WHERE email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashed = rs.getString("password");
                    if (BCrypt.checkpw(password, hashed)) {
                        User user = mapRow(rs);
                        if (!user.isActive()) {
                            return null;
                        }
                        return user;
                    }
                }
            }
        }
        return null;
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("email"),
                User.rolesFromJson(rs.getString("roles")),
                rs.getString("password"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getBoolean("is_active"),
                rs.getString("avatar"),
                rs.getBoolean("is_verified"));
    }
}
