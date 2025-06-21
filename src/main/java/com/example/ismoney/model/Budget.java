// Budget.java
package com.example.ismoney.model;

import java.time.LocalDate;

public class Budget {
    private int id;
    private String category;
    private double limitAmount;
    private double spentAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String period; // "monthly", "weekly", "yearly"
    private boolean isActive;

    // Default constructor
    public Budget() {}

    // Constructor with parameters
    public Budget(String category, double limitAmount, LocalDate startDate, LocalDate endDate, String period) {
        this.category = category;
        this.limitAmount = limitAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period;
        this.spentAmount = 0.0;
        this.isActive = true;
    }

    // Constructor with id
    public Budget(int id, String category, double limitAmount, double spentAmount,
                  LocalDate startDate, LocalDate endDate, String period, boolean isActive) {
        this.id = id;
        this.category = category;
        this.limitAmount = limitAmount;
        this.spentAmount = spentAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.period = period;
        this.isActive = isActive;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }

    public double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // Helper methods
    public double getRemainingAmount() {
        return limitAmount - spentAmount;
    }

    public double getUsagePercentage() {
        if (limitAmount == 0) return 0;
        return (spentAmount / limitAmount) * 100;
    }

    public boolean isOverBudget() {
        return spentAmount > limitAmount;
    }

    public boolean isNearLimit(double threshold) {
        return getUsagePercentage() >= threshold;
    }

    public String getBudgetStatus() {
        if (isOverBudget()) {
            return "OVER_BUDGET";
        } else if (isNearLimit(90)) {
            return "NEAR_LIMIT";
        } else if (isNearLimit(70)) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", category='" + category + '\'' +
                ", limitAmount=" + limitAmount +
                ", spentAmount=" + spentAmount +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", period='" + period + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}