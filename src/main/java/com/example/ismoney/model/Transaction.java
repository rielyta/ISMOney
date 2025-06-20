// Transaction.java
package com.example.ismoney.model;

import java.time.LocalDate;

public class Transaction {
    private int id;
    private String type; // "income" or "expense"
    private double amount;
    private String description;
    private String category;
    private LocalDate date;

    // Default constructor
    public Transaction() {}

    // Constructor with parameters
    public Transaction(String type, double amount, String description, String category, LocalDate date) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.date = date;
    }

    // Constructor with id
    public Transaction(int id, String type, double amount, String description, String category, LocalDate date) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.date = date;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", date=" + date +
                '}';
    }
}