package com.example.ismoney.model;

import java.time.LocalDate;



public class Transaction {
    private String id;
    private String userId;
    private double amount;
    private TransactionType type;
    private Category category;
    private String note;
    private LocalDate date;

    public Transaction(String id, String userId, double amount, TransactionType type,
                       Category category, String note, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.note = note;
        this.date = date;
    }

    // Getter & Setter...
}

