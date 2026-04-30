package com.edukids.services;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.interfaces.IService;
import com.edukids.utils.MyConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserService implements IService<User> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ======================== CRUD ========================

    @Override
    public void add(User user) {
        String sql = "INSERT INTO user (email, roles, password, first_name, last_name, is_active, avatar, is_verified) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.rolesToJson());
            ps.setString(3, BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            ps.setString(4, user.getFirstName());
            ps.setString(5, user.getLastName());
            ps.setBoolean(6, user.isActive());
            ps.setString(7, user.getAvatar());
            ps.setBoolean(8, user.isVerified());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error adding user: " + e.getMessage(), e);
        }
    }

    public void addOAuthUser(User user) {
        String sql = "INSERT INTO user (email, roles, password, first_name, last_name, is_active, avatar, is_verified) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.rolesToJson());
            ps.setString(3, BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt()));
            ps.setString(4, user.getFirstName());
            ps.setString(5, user.getLastName());
            ps.setBoolean(6, user.isActive());
            ps.setString(7, user.getAvatar());
            ps.setBoolean(8, user.isVerified());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error adding OAuth user: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE user SET email=?, roles=?, first_name=?, last_name=?, " +
                     "is_active=?, avatar=?, is_verified=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.rolesToJson());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getLastName());
            ps.setBoolean(5, user.isActive());
            ps.setString(6, user.getAvatar());
            ps.setBoolean(7, user.isVerified());
            ps.setInt(8, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM user WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }

    @Override
    public User getById(int id) {
        String sql = "SELECT * FROM user WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching user: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching users: " + e.getMessage(), e);
        }
        return users;
    }

    // ======================== AUTH ========================

    public User authenticate(String email, String password) {
        String sql = "SELECT * FROM user WHERE email=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    User user = mapResultSetToUser(rs);
                    if (!user.isActive()) {
                        return null;
                    }
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Authentication error: " + e.getMessage(), e);
        }
        return null;
    }

    public void updatePassword(String email, String newPassword) {
        String sql = "UPDATE user SET password=? WHERE email=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating password: " + e.getMessage(), e);
        }
    }

    // ======================== QUERIES ========================

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE email=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking email: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean emailExistsExcluding(String email, int excludeUserId) {
        String sql = "SELECT COUNT(*) FROM user WHERE email=? AND id!=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeUserId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking email: " + e.getMessage(), e);
        }
        return false;
    }

    public User getByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching user by email: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Search users by keyword and optional role filter, with sorting.
     * Matches Symfony's UserRepository::searchUsers()
     */
    public List<User> searchUsers(String keyword, Role roleFilter, String sortBy, String sortOrder) {
        List<User> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM user WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (first_name LIKE ? OR last_name LIKE ? OR email LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (roleFilter != null) {
            sql.append(" AND roles LIKE ?");
            params.add("%" + roleFilter.getDbValue() + "%");
        }

        // Validate sort column
        String validSort = switch (sortBy != null ? sortBy : "id") {
            case "firstName", "first_name" -> "first_name";
            case "lastName", "last_name" -> "last_name";
            case "email" -> "email";
            case "isActive", "is_active" -> "is_active";
            default -> "id";
        };
        String order = "ASC".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
        sql.append(" ORDER BY ").append(validSort).append(" ").append(order);

        try (PreparedStatement ps = cnx.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching users: " + e.getMessage(), e);
        }
        return users;
    }

    /**
     * Find users by specific role. Matches Symfony's UserRepository::findByRole()
     */
    public List<User> findByRole(Role role) {
        return searchUsers(null, role, "id", "DESC");
    }

    public List<User> findByRole(Role role, String keyword) {
        return searchUsers(keyword, role, "id", "DESC");
    }

    // ======================== COUNTS (Dashboard) ========================

    public int countAll() {
        return executeCount("SELECT COUNT(*) FROM user");
    }

    public int countActive() {
        return executeCount("SELECT COUNT(*) FROM user WHERE is_active=1");
    }

    public int countByRole(Role role) {
        String sql = "SELECT COUNT(*) FROM user WHERE roles LIKE ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, "%" + role.getDbValue() + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error counting by role: " + e.getMessage(), e);
        }
        return 0;
    }

    public Map<String, Integer> getRoleCounts() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("Admin", countByRole(Role.ROLE_ADMIN));
        counts.put("Parent", countByRole(Role.ROLE_PARENT));
        counts.put("Eleve", countByRole(Role.ROLE_ELEVE));
        return counts;
    }

    /**
     * Get recent users (last N registered). Matches Symfony admin dashboard.
     */
    public List<User> getRecentUsers(int limit) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching recent users: " + e.getMessage(), e);
        }
        return users;
    }

    // ======================== BLOCK / UNBLOCK ========================

    /**
     * Toggle user active status (ban/unban). Matches Symfony's block() action.
     */
    public void toggleBlock(int userId) {
        String sql = "UPDATE user SET is_active = NOT is_active WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error toggling block status: " + e.getMessage(), e);
        }
    }

    public void toggleVerified(int userId) {
        String sql = "UPDATE user SET is_verified = NOT is_verified WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error toggling verification: " + e.getMessage(), e);
        }
    }

    public void updateAvatar(int userId, String avatarFilename) {
        String sql = "UPDATE user SET avatar=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, avatarFilename);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating avatar: " + e.getMessage(), e);
        }
    }

    // ======================== HELPERS ========================

    private int executeCount(String sql) {
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error counting: " + e.getMessage(), e);
        }
        return 0;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("email"),
            User.rolesFromJson(rs.getString("roles")),
            rs.getString("password"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getBoolean("is_active"),
            rs.getString("avatar"),
            rs.getBoolean("is_verified")
        );
    }
}
