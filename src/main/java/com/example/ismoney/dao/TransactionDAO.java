package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Map;

public class TransactionDAO {
    private DatabaseConfig dbConfig;

    //Inisialisasi koneksi database
    public TransactionDAO() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    //menyimpan transaksi baru ke database
    public boolean saveTransaction(Transaction transaction) {
        String sql = "INSERT INTO transactions (user_id, amount, type, category_id, note, transaction_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, transaction.getUserId());
            stmt.setBigDecimal(2, transaction.getAmount());
            stmt.setString(3, transaction.getType().toString()); // INCOME atau OUTCOME
            stmt.setInt(4, transaction.getCategoryId());
            stmt.setString(5, transaction.getNote());
            stmt.setDate(6, Date.valueOf(transaction.getTransactionDate()));

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Ambil ID yang auto-generated
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    transaction.setTransactionId(generatedKeys.getInt(1));
                }
                System.out.println("Transaction saved successfully with ID: " + transaction.getTransactionId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    //Mengambil semua transksi berdasarkan useer Id
    public List<Transaction> getTransactionsByUserId(Integer userId) {
        List<Transaction> transactions = new ArrayList<>();

        String checkColumnSql = "SELECT column_name FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'created_at'";
        boolean hasCreatedAt = false;

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkColumnSql);
             ResultSet checkRs = checkStmt.executeQuery()) {

            hasCreatedAt = checkRs.next();

        } catch (SQLException e) {
            System.err.println("Error checking column existence: " + e.getMessage());
        }

        String sql;
        if (hasCreatedAt) {
            sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, created_at DESC";
        } else {
            sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, transaction_id DESC";
        }

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

                if (hasCreatedAt) {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        transaction.setCreatedAt(createdAt.toLocalDateTime());
                    }
                }

                transactions.add(transaction);
            }

        } catch (SQLException e) {
            System.err.println("Error getting transactions: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

    //Memperbarui data transaksi yang sudah ada di database
    public boolean updateTransaction(Transaction transaction) {
        String sql = "UPDATE transactions SET amount = ?, type = ?, category_id = ?, note = ?, transaction_date = ? WHERE transaction_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, transaction.getAmount());
            stmt.setString(2, transaction.getType().toString());
            stmt.setInt(3, transaction.getCategoryId());
            stmt.setString(4, transaction.getNote());
            stmt.setDate(5, Date.valueOf(transaction.getTransactionDate()));
            stmt.setInt(6, transaction.getTransactionId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating transaction: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    //Menghapus transaksi dari database berdasarkan transaction ID
    public boolean deleteTransaction(Integer transactionId) {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transactionId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting transaction: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }



    public BigDecimal getTotalExpense(Integer userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM transactions WHERE user_id = ? AND type = 'OUTCOME'";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total expense: " + e.getMessage());
        }

        return BigDecimal.ZERO;
    }

    public List<Transaction> getTransactionsByDate(LocalDate date) {
        String sql = "SELECT * FROM transactions WHERE transaction_date = ? ORDER BY id DESC";
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();

             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getInt("id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setCategoryId(rs.getInt("category_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setType(TransactionType.valueOf(rs.getString("type")));
                transaction.setNote(rs.getString("note"));
                transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                transactions.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    //mendapatkan tranasksi berdasarkan rentang tanggal
    public List<Transaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM transactions WHERE transaction_date BETWEEN ? AND ? ORDER BY transaction_date DESC";
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getInt("transaction_id")); // pakai "id" jika memang field-nya "id"
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setCategoryId(rs.getInt("category_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setType(TransactionType.valueOf(rs.getString("type")));
                transaction.setNote(rs.getString("note"));
                transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    transaction.setCreatedAt(createdAt.toLocalDateTime());
                }

                transactions.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return transactions;
    }

    //mendapatkan tranasksi berdasarkan id user dan bulan transaksi
    public List<Transaction> getTransactionsByUserIdAndMonth(Integer userId, int year, int month) {
        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT transaction_id, user_id, category_id, amount, transaction_date, note, type " +
                "FROM transactions " +
                "WHERE user_id = ? AND EXTRACT(YEAR FROM transaction_date) = ? AND EXTRACT(MONTH FROM transaction_date) = ? " +
                "ORDER BY transaction_date DESC";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, year);
            stmt.setInt(3, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionId(rs.getInt("transaction_id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setCategoryId(rs.getInt("category_id"));
                    transaction.setAmount(rs.getBigDecimal("amount"));
                    transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                    transaction.setNote(rs.getString("note"));

                    String typeStr = rs.getString("type");
                    transaction.setType(TransactionType.valueOf(typeStr.toUpperCase()));

                    transactions.add(transaction);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting transactions by user ID and month: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

    //mendapatkan transaksi yg baru
    public List<Transaction> getRecentTransactionsByUserId(Integer userId, int limit) {
        List<Transaction> transactions = new ArrayList<>();

        String sql = "SELECT transaction_id, user_id, category_id, amount, transaction_date, note, type " +
                "FROM transactions " +
                "WHERE user_id = ? " +
                "ORDER BY transaction_date DESC, transaction_id DESC " +
                "LIMIT ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionId(rs.getInt("transaction_id"));
                    transaction.setUserId(rs.getInt("user_id"));
                    transaction.setCategoryId(rs.getInt("category_id"));
                    transaction.setAmount(rs.getBigDecimal("amount"));
                    transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                    transaction.setNote(rs.getString("note"));

                    // Convert string to TransactionType enum
                    String typeStr = rs.getString("type");
                    transaction.setType(TransactionType.valueOf(typeStr.toUpperCase()));

                    transactions.add(transaction);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting recent transactions: " + e.getMessage());
            e.printStackTrace();
        }

        return transactions;
    }

}