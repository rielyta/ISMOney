package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDAO {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public boolean insertUser(String email, String password) {
        String sql = "INSERT INTO users (email, password) VALUES (?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkCredentials(String email, String password) {
        String sql = "SELECT * FROM users WHERE email=? AND password=?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean insertGoogleUser(String email, String googleId) {
        String sql = "INSERT INTO users (email, google_id) VALUES (?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, googleId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkGoogleCredentials(String email, String googleId) {
        String sql = "SELECT * FROM users WHERE email=? AND google_id=?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, googleId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
