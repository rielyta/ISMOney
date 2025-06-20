// TransactionService.java
package com.example.ismoney.service;

import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.model.Transaction;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class TransactionService {
    private final TransactionDAO transactionDAO;

    public TransactionService() {
        this.transactionDAO = new TransactionDAO();
    }

    public void addTransaction(Transaction transaction) throws SQLException {
        validateTransaction(transaction);
        transactionDAO.saveTransaction(transaction);
    }

    public List<Transaction> getAllTransactions(int userId) throws SQLException {
        return transactionDAO.getTransactionsByUserId(userId);
    }

    public void updateTransaction(Transaction transaction) throws SQLException {
        validateTransaction(transaction);
        transactionDAO.updateTransaction(transaction);
    }

    public void deleteTransaction(int id) throws SQLException {
        transactionDAO.deleteTransaction(id);
    }

    private void validateTransaction(Transaction transaction) {
        if (transaction.getAmount() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        if (transaction.getType() == null ||
                (!transaction.getType().equals("income") && !transaction.getType().equals("expense"))) {
            throw new IllegalArgumentException("Transaction type must be 'income' or 'expense'");
        }

        if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction description cannot be empty");
        }

        if (transaction.getDate() == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
    }

    // Inner class for transaction summary
    public static class TransactionSummary {
        private double totalIncome;
        private double totalExpense;
        private double netIncome;

        public TransactionSummary(double totalIncome, double totalExpense, double netIncome) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.netIncome = netIncome;
        }

        public double getTotalIncome() { return totalIncome; }
        public double getTotalExpense() { return totalExpense; }
        public double getNetIncome() { return netIncome; }
    }
}