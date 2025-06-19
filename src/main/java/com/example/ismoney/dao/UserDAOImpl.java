package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.user.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class UserDAOImpl implements UserDAO {
    private static final Logger logger = Logger.getLogger(UserDAOImpl.class.getName());

    @Override
    public User findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.warning("Email parameter is null or empty");
            return null;
        }

        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim().toLowerCase());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while finding user by email", e);
        }
        return null;
    }

    @Override
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warning("Username parameter is null or empty");
            return null;
        }

        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while finding user by username", e);
        }
        return null;
    }

    @Override
    public boolean save(User userObj) {
        if (userObj == null) {
            logger.warning("User object is null");
            return false;
        }

        if (!isValidUser(userObj)) {
            logger.warning("Invalid user data provided for save operation");
            return false;
        }

        String sql = "INSERT INTO users (username, email, password_hash, created_at) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, userObj.getUsername().trim());
            stmt.setString(2, userObj.getEmail().trim().toLowerCase());
            stmt.setString(3, userObj.getPasswordHash());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    userObj.setId(generatedKeys.getInt(1));
                }
                logger.info("User saved successfully with ID: " + userObj.getId());
                return true;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while saving user", e);
        }
        return false;
    }

    @Override
    public boolean isEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim().toLowerCase());
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while checking email existence", e);
            return false;
        }
    }

    @Override
    public boolean isUsernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while checking username existence", e);
            return false;
        }
    }

    @Override
    public User authenticateUser(String emailOrUsername, String password) {
        if (emailOrUsername == null || password == null ||
                emailOrUsername.trim().isEmpty() || password.isEmpty()) {
            logger.warning("Invalid credentials provided for authentication");
            return null;
        }

        User user = null;
        String trimmedInput = emailOrUsername.trim();

        if (trimmedInput.contains("@")) {
            user = findByEmail(trimmedInput);
        } else {
            user = findByUsername(trimmedInput);
        }

        if (user == null) {
            logger.info("User not found for login attempt: " + trimmedInput);
            return null;
        }

        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            logger.info("Successful authentication for user: " + user.getUsername());
            return user;
        } else {
            logger.warning("Failed authentication attempt for user: " + user.getUsername());
            return null;
        }
    }

    @Override
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    @Override
    public boolean updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error while updating last login", e);
            return false;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));

        try {
            user.setCreatedAt(rs.getTimestamp("created_at"));
            user.setLastLogin(rs.getTimestamp("last_login"));
        } catch (SQLException e) {
        }

        return user;
    }

    private boolean isValidUser(User user) {
        return user.getUsername() != null && !user.getUsername().trim().isEmpty() &&
                user.getEmail() != null && !user.getEmail().trim().isEmpty() &&
                user.getPasswordHash() != null && !user.getPasswordHash().isEmpty() &&
                isValidEmail(user.getEmail()) &&
                isValidUsername(user.getUsername());
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    private boolean isValidUsername(String username) {
        return username != null &&
                username.length() >= 3 &&
                username.length() <= 20 &&
                username.matches("^[a-zA-Z0-9_]+$");
    }
}