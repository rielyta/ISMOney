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

    // method untuk nambah tabungan ke goal
    public boolean addSavingToGoal(int goalId, BigDecimal amount) {
        String sql = "UPDATE saving_goals SET current_amount = current_amount + ? WHERE goal_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, amount);
            stmt.setInt(2, goalId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error adding saving to goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // method utk update status otomatis berdasarkan progres
    public void updateGoalStatusBasedOnProgress(int goalId, String status) {
        String sql = "UPDATE saving_goals SET status = ? WHERE id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, goalId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating goal status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // CREATE
    public boolean addSavingGoal(SavingGoal goal) {
        String sql = "INSERT INTO saving_goals (goal_name, target_amount, current_amount, target_date, created_date, description, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, goal.getGoalName());
            stmt.setBigDecimal(2, goal.getTargetAmount());
            stmt.setBigDecimal(3, goal.getCurrentAmount());
            stmt.setDate(4, Date.valueOf(goal.getTargetDate()));
            stmt.setDate(5, Date.valueOf(goal.getCreatedDate()));
            stmt.setString(6, goal.getDescription());
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
    public List<SavingGoal> getAllSavingGoals() {
        List<SavingGoal> goals = new ArrayList<>();
        String sql = "SELECT goal_id, goal_name, target_amount, current_amount, target_date, created_date, description, status FROM saving_goals ORDER BY created_date DESC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                SavingGoal goal = new SavingGoal();
                goal.setGoalId(rs.getInt("goal_id"));
                goal.setGoalName(rs.getString("goal_name"));
                goal.setTargetAmount(rs.getBigDecimal("target_amount"));
                goal.setCurrentAmount(rs.getBigDecimal("current_amount"));
                goal.setTargetDate(rs.getDate("target_date").toLocalDate());
                goal.setCreatedDate(rs.getDate("created_date").toLocalDate());
                goal.setDescription(rs.getString("description"));
                goal.setStatus(rs.getString("status"));
                goals.add(goal);
            }

        } catch (SQLException e) {
            System.err.println("Error getting saving goals: " + e.getMessage());
            e.printStackTrace();
        }

        return goals;
    }

    public SavingGoal getSavingGoalById(int goalId) {
        String sql = "SELECT goal_id, goal_name, target_amount, current_amount, target_date, created_date, description, status FROM saving_goals WHERE goal_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, goalId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                SavingGoal goal = new SavingGoal();
                goal.setGoalId(rs.getInt("goal_id"));
                goal.setGoalName(rs.getString("goal_name"));
                goal.setTargetAmount(rs.getBigDecimal("target_amount"));
                goal.setCurrentAmount(rs.getBigDecimal("current_amount"));
                goal.setTargetDate(rs.getDate("target_date").toLocalDate());
                goal.setCreatedDate(rs.getDate("created_date").toLocalDate());
                goal.setDescription(rs.getString("description"));
                goal.setStatus(rs.getString("status"));
                return goal;
            }

        } catch (SQLException e) {
            System.err.println("Error getting saving goal by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // UPDATE
    public boolean updateSavingGoal(SavingGoal goal) {
        String sql = "UPDATE saving_goals SET goal_name = ?, target_amount = ?, current_amount = ?, target_date = ?, description = ?, status = ? WHERE goal_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, goal.getGoalName());
            stmt.setBigDecimal(2, goal.getTargetAmount());
            stmt.setBigDecimal(3, goal.getCurrentAmount());
            stmt.setDate(4, Date.valueOf(goal.getTargetDate()));
            stmt.setString(5, goal.getDescription());
            stmt.setString(6, goal.getStatus());
            stmt.setInt(7, goal.getGoalId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating saving goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // DELETE
    public boolean deleteSavingGoal(int goalId) {
        String sql = "DELETE FROM saving_goals WHERE goal_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, goalId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting saving goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public List<SavingGoal> getActiveSavingGoals() throws SQLException {
        List<SavingGoal> savingGoals = new ArrayList<>();
        String sql = "SELECT * FROM saving_goals WHERE status = 'active' ORDER BY target_date ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SavingGoal savingGoal = new SavingGoal(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("target_amount"),
                            rs.getDouble("current_amount"),
                            rs.getDate("target_date").toLocalDate(),
                            rs.getDate("created_date").toLocalDate(),
                            rs.getString("status")
                    );
                    savingGoals.add(savingGoal);
                }
            }
        }
        return savingGoals;
    }
}