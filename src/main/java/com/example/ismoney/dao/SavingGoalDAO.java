package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.SavingGoal;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavingGoalDAO {
    private DatabaseConfig dbConfig;

    public SavingGoalDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    // Utility method to map ResultSet to SavingGoal
    private SavingGoal mapResultSetToSavingGoal(ResultSet rs) throws SQLException {
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

    // READ - Get all saving goals by user ID
    public List<SavingGoal> getSavingGoalsByUserId(Integer userId) throws SQLException {
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status FROM saving_goals WHERE user_id = ? ORDER BY created_date DESC";

        List<SavingGoal> goals = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SavingGoal goal = mapResultSetToSavingGoal(rs);
                    goals.add(goal);
                }
            }
        }

        return goals;
    }

    // READ - Get active saving goals by user ID (FIXED - removed duplicate method)
    public List<SavingGoal> getActiveSavingGoalsByUserId(Integer userId) throws SQLException {
        List<SavingGoal> savingGoals = new ArrayList<>();
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status FROM saving_goals WHERE user_id = ? AND status = 'ACTIVE' ORDER BY target_date ASC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                SavingGoal savingGoal = mapResultSetToSavingGoal(rs);
                savingGoals.add(savingGoal);
            }

        } catch (SQLException e) {
            System.err.println("Error getting active saving goals: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to maintain SQLException contract
        }

        return savingGoals;
    }

    // READ - Get single saving goal by ID and user ID
    public SavingGoal getSavingGoalById(int goalId, Integer userId) {
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status FROM saving_goals WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, goalId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToSavingGoal(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error getting saving goal by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // READ - Method yang dibutuhkan oleh DashboardController (MAIN METHOD FOR DASHBOARD)
    public List<SavingGoal> getRecentUpdatedGoalsByUserId(Integer userId, int limit) {
        List<SavingGoal> goals = new ArrayList<>();
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status " +
                "FROM saving_goals WHERE user_id = ? AND current_amount > 0 ORDER BY created_date DESC LIMIT ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                SavingGoal goal = mapResultSetToSavingGoal(rs);
                goals.add(goal);
            }

            System.out.println("Retrieved " + goals.size() + " recent updated goals for user " + userId);

        } catch (SQLException e) {
            System.err.println("Error getting recent updated goals: " + e.getMessage());
            e.printStackTrace();
        }

        return goals;
    }

    // UPDATE - Update saving goal
    public boolean updateSavingGoal(SavingGoal goal) {
        String sql = "UPDATE saving_goals SET goal_name = ?, target_amount = ?, current_amount = ?, target_date = ?, status = ? WHERE goal_id = ? AND user_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, goal.getGoalName());
            stmt.setBigDecimal(2, goal.getTargetAmount());
            stmt.setBigDecimal(3, goal.getCurrentAmount());
            stmt.setDate(4, Date.valueOf(goal.getTargetDate()));
            stmt.setString(5, goal.getStatus());
            stmt.setInt(6, goal.getGoalId());
            stmt.setInt(7, goal.getUserId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating saving goal: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // UPDATE - utk menambah tabungan ke goal
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

    // UPDATE - Method untuk update status otomatis berdasarkan progres
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

    // DELETE - Delete saving goal
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
            return false;
        }
    }

    // ANALYTICS - Method untuk mendapatkan total target amount berdasarkan user
    public BigDecimal getTotalTargetAmountByUserId(Integer userId) {
        String sql = "SELECT SUM(target_amount) as total FROM saving_goals WHERE user_id = ? AND status = 'ACTIVE'";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            }

        } catch (SQLException e) {
            System.err.println("Error getting total target amount: " + e.getMessage());
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    // ANALYTICS - Method untuk mendapatkan total current amount berdasarkan user
    public BigDecimal getTotalCurrentAmountByUserId(Integer userId) {
        String sql = "SELECT SUM(current_amount) as total FROM saving_goals WHERE user_id = ? AND status = 'ACTIVE'";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal("total");
                return total != null ? total : BigDecimal.ZERO;
            }

        } catch (SQLException e) {
            System.err.println("Error getting total current amount: " + e.getMessage());
            e.printStackTrace();
        }

        return BigDecimal.ZERO;
    }

    // ANALYTICS - Method untuk mendapatkan jumlah goal aktif berdasarkan user
    public int getActiveGoalsCountByUserId(Integer userId) {
        String sql = "SELECT COUNT(*) as count FROM saving_goals WHERE user_id = ? AND status = 'ACTIVE'";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("Error getting active goals count: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    // ANALYTICS - Method untuk mendapatkan goal yang mendekati deadline
    public List<SavingGoal> getGoalsNearDeadline(Integer userId, int daysFromNow) {
        List<SavingGoal> goals = new ArrayList<>();
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, current_amount, target_date, created_date, status " +
                "FROM saving_goals WHERE user_id = ? AND status = 'ACTIVE' AND target_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY) " +
                "ORDER BY target_date ASC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, daysFromNow);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                SavingGoal goal = mapResultSetToSavingGoal(rs);
                goals.add(goal);
            }

        } catch (SQLException e) {
            System.err.println("Error getting goals near deadline: " + e.getMessage());
            e.printStackTrace();
        }

        return goals;
    }

    // MAINTENANCE - Method untuk auto-update status goal yang sudah mencapai target
    public boolean autoUpdateCompletedGoals(Integer userId) {
        String sql = "UPDATE saving_goals SET status = 'COMPLETED' WHERE user_id = ? AND current_amount >= target_amount AND status = 'ACTIVE'";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int updatedRows = stmt.executeUpdate();

            if (updatedRows > 0) {
                System.out.println("Auto-updated " + updatedRows + " completed goals for user " + userId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error auto-updating completed goals: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ANALYTICS - Method untuk mendapatkan statistik goal berdasarkan status
    public Map<String, Integer> getGoalStatsByUserId(Integer userId) {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as count FROM saving_goals WHERE user_id = ? GROUP BY status";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                stats.put(rs.getString("status"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            System.err.println("Error getting goal stats: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    // DEPRECATED METHODS - Maintained for backward compatibility but should not be used
    @Deprecated
    public List<SavingGoal> getRecentUpdatedGoals(int limit) {
        System.err.println("WARNING: getRecentUpdatedGoals(int) is deprecated and should not be used. Use getRecentUpdatedGoalsByUserId(Integer, int) instead.");
        return new ArrayList<>(); // Return empty list to prevent data leakage
    }
}