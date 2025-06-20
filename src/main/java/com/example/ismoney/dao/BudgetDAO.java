// BudgetDAO.java
package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.Budget;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BudgetDAO {

    public void saveBudget(Budget budget) throws SQLException {
        String sql = "INSERT INTO budgets (category, limit_amount, spent_amount, start_date, end_date, period, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, budget.getCategory());
            stmt.setDouble(2, budget.getLimitAmount());
            stmt.setDouble(3, budget.getSpentAmount());
            stmt.setDate(4, Date.valueOf(budget.getStartDate()));
            stmt.setDate(5, Date.valueOf(budget.getEndDate()));
            stmt.setString(6, budget.getPeriod());
            stmt.setBoolean(7, budget.isActive());

            stmt.executeUpdate();
        }
    }

    public List<Budget> getAllBudgets() throws SQLException {
        List<Budget> budgets = new ArrayList<>();
        String sql = "SELECT * FROM budgets ORDER BY created_date DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Budget budget = new Budget(
                        rs.getInt("id"),
                        rs.getString("category"),
                        rs.getDouble("limit_amount"),
                        rs.getDouble("spent_amount"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getString("period"),
                        rs.getBoolean("is_active")
                );
                budgets.add(budget);
            }
        }
        return budgets;
    }

    public List<Budget> getActiveBudgets() throws SQLException {
        List<Budget> budgets = new ArrayList<>();
        String sql = "SELECT * FROM budgets WHERE is_active = true ORDER BY category ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Budget budget = new Budget(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("limit_amount"),
                            rs.getDouble("spent_amount"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getString("period"),
                            rs.getBoolean("is_active")
                    );
                    budgets.add(budget);
                }
            }
        }
        return budgets;
    }

    public List<Budget> getCurrentBudgets() throws SQLException {
        List<Budget> budgets = new ArrayList<>();
        String sql = "SELECT * FROM budgets WHERE is_active = true AND start_date <= ? AND end_date >= ? ORDER BY category ASC";
        LocalDate today = LocalDate.now();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(today));
            stmt.setDate(2, Date.valueOf(today));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Budget budget = new Budget(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("limit_amount"),
                            rs.getDouble("spent_amount"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getString("period"),
                            rs.getBoolean("is_active")
                    );
                    budgets.add(budget);
                }
            }
        }
        return budgets;
    }

    public Budget getBudgetById(int id) throws SQLException {
        String sql = "SELECT * FROM budgets WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Budget(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("limit_amount"),
                            rs.getDouble("spent_amount"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getString("period"),
                            rs.getBoolean("is_active")
                    );
                }
            }
        }
        return null;
    }

    public Budget getBudgetByCategory(String category, LocalDate date) throws SQLException {
        String sql = "SELECT * FROM budgets WHERE category = ? AND is_active = true AND start_date <= ? AND end_date >= ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Budget(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("limit_amount"),
                            rs.getDouble("spent_amount"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getString("period"),
                            rs.getBoolean("is_active")
                    );
                }
            }
        }
        return null;
    }

    public void updateBudget(Budget budget) throws SQLException {
        String sql = "UPDATE budgets SET category = ?, limit_amount = ?, spent_amount = ?, start_date = ?, end_date = ?, period = ?, is_active = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, budget.getCategory());
            stmt.setDouble(2, budget.getLimitAmount());
            stmt.setDouble(3, budget.getSpentAmount());
            stmt.setDate(4, Date.valueOf(budget.getStartDate()));
            stmt.setDate(5, Date.valueOf(budget.getEndDate()));
            stmt.setString(6, budget.getPeriod());
            stmt.setBoolean(7, budget.isActive());
            stmt.setInt(8, budget.getId());

            stmt.executeUpdate();
        }
    }

    public void updateBudgetSpent(int budgetId, double spentAmount) throws SQLException {
        String sql = "UPDATE budgets SET spent_amount = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, spentAmount);
            stmt.setInt(2, budgetId);

            stmt.executeUpdate();
        }
    }

    public void addToBudgetSpent(int budgetId, double amount) throws SQLException {
        String sql = "UPDATE budgets SET spent_amount = spent_amount + ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, amount);
            stmt.setInt(2, budgetId);

            stmt.executeUpdate();
        }
    }

    public void deleteBudget(int id) throws SQLException {
        String sql = "DELETE FROM budgets WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void deactivateBudget(int id) throws SQLException {
        String sql = "UPDATE budgets SET is_active = false WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // Calculate total spent for category in date range
    public double getTotalSpentByCategory(String category, LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM transactions WHERE category = ? AND type = 'expense' AND date BETWEEN ? AND ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }
}