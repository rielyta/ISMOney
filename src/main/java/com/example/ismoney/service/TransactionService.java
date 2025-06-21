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