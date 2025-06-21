package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.SavingGoal;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SavingGoalDAO {
    private DatabaseConfig dbConfig;

    public SavingGoalDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    // utk menambah tabungan ke goal
    public boolean addSavingToGoal(int goalId, BigDecimal amount, Integer userId) {
        String sql = "UPDATE saving_goals SET current_amount = current_amount + ? WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setInt(2, goalId);
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error adding saving to goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // Method untuk update status otomatis berdasarkan progres
    public boolean updateGoalStatusBasedOnProgress(int goalId, String status, Integer userId) {
        String sql = "UPDATE saving_goals SET status = ? WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, goalId);
            stmt.setInt(3, userId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating goal status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // CREATE - Menambah goal baru dengan user_id
    public boolean addSavingGoal(SavingGoal goal) {
        String sql = "INSERT INTO saving_goals (user_id, goal_name, target_amount, current_amount, target_date, created_date, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, goal.getUserId());
            stmt.setString(2, goal.getGoalName());
            stmt.setBigDecimal(3, goal.getTargetAmount());
            stmt.setBigDecimal(4, goal.getCurrentAmount());
            stmt.setDate(5, Date.valueOf(goal.getTargetDate()));
            stmt.setDate(6, Date.valueOf(goal.getCreatedDate()));
            stmt.setString(7, goal.getStatus());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    goal.setGoalId(generatedKeys.getInt(1));
                }
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error adding saving goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // READ
    public List<SavingGoal> getSavingGoalsByUserId(Integer userId) {
        List<SavingGoal> goals = new ArrayList<>();
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status FROM saving_goals WHERE user_id = ? ORDER BY created_date DESC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                SavingGoal goal = new SavingGoal();
                goal.setGoalId(rs.getInt("goal_id"));
                goal.setUserId(rs.getInt("user_id"));
                goal.setGoalName(rs.getString("goal_name"));
                goal.setTargetAmount(rs.getBigDecimal("target_amount"));
                goal.setCurrentAmount(rs.getBigDecimal("current_amount"));
                goal.setTargetDate(rs.getDate("target_date").toLocalDate());
                goal.setCreatedDate(rs.getDate("created_date").toLocalDate());
                goal.setStatus(rs.getString("status"));
                goals.add(goal);
            }

        } catch (SQLException e) {
            System.err.println("Error getting saving goals by user ID: " + e.getMessage());
            e.printStackTrace();
        }

        return goals;
    }

    // READ
    public SavingGoal getSavingGoalById(int goalId, Integer userId) {
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status FROM saving_goals WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, goalId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                SavingGoal goal = new SavingGoal();
                goal.setGoalId(rs.getInt("goal_id"));
                goal.setUserId(rs.getInt("user_id"));
                goal.setGoalName(rs.getString("goal_name"));
                goal.setTargetAmount(rs.getBigDecimal("target_amount"));
                goal.setCurrentAmount(rs.getBigDecimal("current_amount"));
                goal.setTargetDate(rs.getDate("target_date").toLocalDate());
                goal.setCreatedDate(rs.getDate("created_date").toLocalDate());
                goal.setStatus(rs.getString("status"));
                return goal;
            }

        } catch (SQLException e) {
            System.err.println("Error getting saving goal by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // READ - Mengambil goal aktif berdasarkan user_id
    public List<SavingGoal> getActiveSavingGoalsByUserId(Integer userId) {
        List<SavingGoal> savingGoals = new ArrayList<>();
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status FROM saving_goals WHERE user_id = ? AND status = 'ACTIVE' ORDER BY target_date ASC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                SavingGoal savingGoal = new SavingGoal();
                savingGoal.setGoalId(rs.getInt("goal_id"));
                savingGoal.setUserId(rs.getInt("user_id"));
                savingGoal.setGoalName(rs.getString("goal_name"));
                savingGoal.setTargetAmount(rs.getBigDecimal("target_amount"));
                savingGoal.setCurrentAmount(rs.getBigDecimal("current_amount"));
                savingGoal.setTargetDate(rs.getDate("target_date").toLocalDate());
                savingGoal.setCreatedDate(rs.getDate("created_date").toLocalDate());
                savingGoal.setStatus(rs.getString("status"));
                savingGoals.add(savingGoal);
            }

        } catch (SQLException e) {
            System.err.println("Error getting active saving goals: " + e.getMessage());
            e.printStackTrace();
        }

        return savingGoals;
    }

    // READ
    public List<SavingGoal> getRecentUpdatedGoalsByUserId(Integer userId, int limit) {
        List<SavingGoal> goals = new ArrayList<>();
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status " +
                "FROM saving_goals WHERE user_id = ? AND current_amount > 0 ORDER BY created_date DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                SavingGoal goal = new SavingGoal();
                goal.setGoalId(rs.getInt("goal_id"));
                goal.setUserId(rs.getInt("user_id"));
                goal.setGoalName(rs.getString("goal_name"));
                goal.setTargetAmount(rs.getBigDecimal("target_amount"));
                goal.setCurrentAmount(rs.getBigDecimal("current_amount"));
                goal.setTargetDate(rs.getDate("target_date").toLocalDate());
                goal.setCreatedDate(rs.getDate("created_date").toLocalDate());
                goal.setStatus(rs.getString("status"));
                goals.add(goal);
            }

        } catch (SQLException e) {
            System.err.println("Error getting recent updated goals: " + e.getMessage());
            e.printStackTrace();
        }

        return goals;
    }

    // UPDATE
    public boolean updateSavingGoal(SavingGoal goal) {
        String sql = "UPDATE saving_goals SET goal_name = ?, target_amount = ?, current_amount = ?, target_date = ?, status = ? WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, goal.getGoalName());
            stmt.setBigDecimal(2, goal.getTargetAmount());
            stmt.setBigDecimal(3, goal.getCurrentAmount());
            stmt.setDate(4, Date.valueOf(goal.getTargetDate()));
            stmt.setString(6, goal.getStatus());
            stmt.setInt(7, goal.getGoalId());
            stmt.setInt(8, goal.getUserId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating saving goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // DELETE
    public boolean deleteSavingGoal(int goalId, Integer userId) {
        String sql = "DELETE FROM saving_goals WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, goalId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting saving goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    @Deprecated
    public List<SavingGoal> getAllSavingGoals() {
        System.err.println("Warning: getAllSavingGoals() is deprecated. Use getSavingGoalsByUserId() instead.");
        return new ArrayList<>();
    }

    @Deprecated
    public List<SavingGoal> getActiveSavingGoals() throws SQLException {
        System.err.println("Warning: getActiveSavingGoals() is deprecated. Use getActiveSavingGoalsByUserId() instead.");
        return new ArrayList<>();
    }

    @Deprecated
    public SavingGoal getSavingGoalById(int id) throws SQLException {
        System.err.println("Warning: getSavingGoalById(int) is deprecated. Use getSavingGoalById(int, Integer) instead.");
        return null;
    }
}