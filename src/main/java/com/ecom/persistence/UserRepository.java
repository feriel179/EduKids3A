package com.ecom.persistence;

import com.ecom.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class UserRepository {

    public User findByEmail(String email) throws SQLException {
        String sql = """
                SELECT *
                FROM `user`
                WHERE email = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    return new User(
                            resultSet.getInt("id"),
                            resultSet.getString("email"),
                            getColumnValue(resultSet, metaData, "role", "roles"),
                            getColumnValue(resultSet, metaData, "password_hash", "password"),
                            resultSet.getString("first_name"),
                            resultSet.getString("last_name"),
                            resultSet.getBoolean("is_active")
                    );
                }
            }
        }

        return null;
    }

    private String getColumnValue(ResultSet resultSet, ResultSetMetaData metaData, String preferredColumn, String fallbackColumn)
            throws SQLException {
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            if (preferredColumn.equalsIgnoreCase(columnName)) {
                return resultSet.getString(preferredColumn);
            }
        }

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            if (fallbackColumn.equalsIgnoreCase(columnName)) {
                return resultSet.getString(fallbackColumn);
            }
        }

        return null;
    }
}
