// TransactionService.java
package com.example.ismoney.service;

import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.model.Transaction;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import com.example.ismoney.model.TransactionType;
import java.math.BigDecimal;

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
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        if (transaction.getType() == null ||
                (!transaction.getType().equals("income") && !transaction.getType().equals("expense"))) {
            throw new IllegalArgumentException("Transaction type must be 'income' or 'expense'");
        }

        if (transaction.getNote() == null || transaction.getNote().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction note cannot be empty");
        }

        if (transaction.getTransactionDate() == null) {
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

    public double getTotalIncomeByDate(LocalDate date) throws SQLException {
        return transactionDAO.getTransactionsByDate(date).stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    public double getTotalExpenseByDate(LocalDate date) throws SQLException {
        return transactionDAO.getTransactionsByDate(date).stream()
                .filter(t -> t.getType() == TransactionType.OUTCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    public List<Transaction> getTransactionsByDate(LocalDate date) {
        return transactionDAO.getTransactionsByDate(date);
    }

    public List<Transaction> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactionDAO.getTransactionsByDateRange(startDate, endDate);
    }

}