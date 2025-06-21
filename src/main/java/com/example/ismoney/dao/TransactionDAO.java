package com.example.ismoney.dao;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;

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

            //Set Parameter untuk prepared statement
            stmt.setInt(1, transaction.getUserId());
            stmt.setBigDecimal(2, transaction.getAmount());
            stmt.setString(3, transaction.getType().toString()); // INCOME atau OUTCOME
            stmt.setInt(4, transaction.getCategoryId());
            stmt.setString(5, transaction.getNote());
            stmt.setDate(6, Date.valueOf(transaction.getTransactionDate()));

            //Eksekusi query insert
            int rowsAffected = stmt.executeUpdate();

            //Insert berhasil lalu ambil auto-generated ID
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

        // Cek apakah kolom created_at ada di tabel
        String checkColumnSql = "SELECT column_name FROM information_schema.columns WHERE table_name = 'transactions' AND column_name = 'created_at'";
        boolean hasCreatedAt = false;

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkColumnSql);
             ResultSet checkRs = checkStmt.executeQuery()) {

            hasCreatedAt = checkRs.next();

        } catch (SQLException e) {
            System.err.println("Error checking column existence: " + e.getMessage());
        }

        // Tentukan query berdasarkan ketersediaan kolom created_at
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

            // Mapping hasil query ke object Transaction
            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setTransactionId(rs.getInt("transaction_id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setAmount(rs.getBigDecimal("amount"));
                transaction.setType(TransactionType.valueOf(rs.getString("type")));
                transaction.setCategoryId(rs.getInt("category_id"));
                transaction.setNote(rs.getString("note"));
                transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());

                // Set created_at jika kolom tersebut ada
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
        // SQL query untuk update transaksi berdasarkan transaction_id
        String sql = "UPDATE transactions SET amount = ?, type = ?, category_id = ?, note = ?, transaction_date = ? WHERE transaction_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Set parameter untuk update
            stmt.setBigDecimal(1, transaction.getAmount());
            stmt.setString(2, transaction.getType().toString());
            stmt.setInt(3, transaction.getCategoryId());
            stmt.setString(4, transaction.getNote());
            stmt.setDate(5, Date.valueOf(transaction.getTransactionDate()));
            stmt.setInt(6, transaction.getTransactionId());

            // Return true jika ada row yang ter-update
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error updating transaction: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    //Menghapus transaksi dari database berdasarkan transaction ID
    public boolean deleteTransaction(Integer transactionId) {
        // SQL query untuk delete transaksi berdasarkan transaction_id
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Return true jika ada row yang terhapus
            stmt.setInt(1, transactionId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting transaction: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public BigDecimal getTotalIncome(Integer userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM transactions WHERE user_id = ? AND type = 'INCOME'";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total income: " + e.getMessage());
        }

        return BigDecimal.ZERO;
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

}