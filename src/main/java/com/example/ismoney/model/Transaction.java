package com.example.ismoney.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Transaction {
    private Integer transactionId;  // Sesuai dengan database (transaction_id)
    private Integer userId;         // Sesuai dengan database (user_id)
    private BigDecimal amount;      // Sesuai dengan DECIMAL di database
    private TransactionType type;
    private Integer categoryId;     // Sesuai dengan database (category_id)
    private String note;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;

    public Transaction() {}

    public Transaction(Integer transactionId, Integer userId, BigDecimal amount,
                       TransactionType type, Integer categoryId, String note, LocalDate transactionDate) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.note = note;
        this.transactionDate = transactionDate;
    }

    // Getters and Setters
    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", amount=" + amount +
                ", type=" + type +
                ", categoryId=" + categoryId +
                ", note='" + note + '\'' +
                ", transactionDate=" + transactionDate +
                '}';
    }
}