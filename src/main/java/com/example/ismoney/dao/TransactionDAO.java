package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {
    private DatabaseConfig dbConfig;

    public TransactionDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    // Create - Insert new transaction
    public boolean saveTransaction(Transaction transaction) {
        String sql = """
            INSERT INTO transactions (user_id, amount, type, category_id, note, transaction_date)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transaction.getUserId());
            stmt.setBigDecimal(2, transaction.getAmount());
            stmt.setString(3, transaction.getType().toString());
            stmt.setInt(4, transaction.getCategoryId());
            stmt.setString(5, transaction.getNote());
            stmt.setDate(6, Date.valueOf(transaction.getTransactionDate()));

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Read - Get all transactions for a user
    public List<Transaction> getTransactionsByUserId(int userId) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT transaction_id, user_id, amount, type, category_id, note, transaction_date, created_at
            FROM transactions 
            WHERE user_id = ?
            ORDER BY transaction_date DESC, created_at DESC
        """;

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getInt("transaction_id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setType(TransactionType.valueOf(rs.getString("type")));
                transaction.setCategoryId(rs.getInt("category_id"));
                transaction.setNote(rs.getString("note"));
                transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                transactions.add(transaction);
            }

        } catch (SQLException e) {
            System.err.println("Error getting transactions: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

    // Read - Get transaction by ID
    public Transaction getTransactionById(int transactionId) {
        String sql = """
            SELECT transaction_id, user_id, amount, type, category_id, note, transaction_date, created_at
            FROM transactions 
            WHERE transaction_id = ?
        """;

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transactionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getInt("transaction_id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setType(TransactionType.valueOf(rs.getString("type")));
                transaction.setCategoryId(rs.getInt("category_id"));
                transaction.setNote(rs.getString("note"));
                transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                return transaction;
            }

        } catch (SQLException e) {
            System.err.println("Error getting transaction by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateTransaction(Transaction transaction) {
        String sql = """
            UPDATE transactions 
            SET amount = ?, type = ?, category_id = ?, note = ?, transaction_date = ?
            WHERE transaction_id = ?
        """;

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, transaction.getAmount());
            stmt.setString(2, transaction.getType().toString());
            stmt.setInt(3, transaction.getCategoryId());
            stmt.setString(4, transaction.getNote());
            stmt.setDate(5, Date.valueOf(transaction.getTransactionDate()));
            stmt.setInt(6, transaction.getTransactionId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteTransaction(int transactionId) {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transactionId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Transaction> getTransactionsByType(int userId, TransactionType type) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT transaction_id, user_id, amount, type, category_id, note, transaction_date, created_at
            FROM transactions 
            WHERE user_id = ? AND type = ?
            ORDER BY transaction_date DESC, created_at DESC
        """;

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, type.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getInt("transaction_id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setType(TransactionType.valueOf(rs.getString("type")));
                transaction.setCategoryId(rs.getInt("category_id"));
                transaction.setNote(rs.getString("note"));
                transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                transactions.add(transaction);
            }

        } catch (SQLException e) {
            System.err.println("Error getting transactions by type: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }
}