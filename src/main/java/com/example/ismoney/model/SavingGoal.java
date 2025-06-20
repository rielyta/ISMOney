package com.example.ismoney.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class SavingGoal {
    private Integer goalId;
    private Integer userId;
    private String goalName;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private LocalDate createdDate;
    private String status;
    private LocalDate updatedAt;

    public SavingGoal() {
        this.currentAmount = BigDecimal.ZERO;
        this.createdDate = LocalDate.now();
        this.updatedAt = LocalDate.now();
        this.status = "ACTIVE";
    }

    // utkpemanggilan dari DAO
    public SavingGoal(Integer goalId, Integer userId, String goalName, BigDecimal targetAmount,
                      BigDecimal currentAmount, LocalDate targetDate, LocalDate createdDate, String status) {
        this.goalId = goalId;
        this.userId = userId;
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.targetDate = targetDate;
        this.createdDate = createdDate;
        this.status = status;
    }

    @Override
    public String toString() {
        return goalName;
    }

    //untuk progres dan status
    public double getProgressPercentage() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return currentAmount.divide(targetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    public BigDecimal getRemainingAmount() {
        return targetAmount.subtract(currentAmount);
    }

    public boolean isCompleted() {
        return currentAmount.compareTo(targetAmount) >= 0;
    }

    public boolean isOverdue() {
        return LocalDate.now().isAfter(targetDate) && !isCompleted();
    }

    public String getProgressStatus() {
        if (isCompleted()) {
            return "SELESAI";
        } else if (isOverdue()) {
            return "TERLAMBAT";
        } else {
            return "DALAM_PROGRES";
        }
    }

    public Integer getGoalId() { return goalId; }
    public void setGoalId(Integer goalId) { this.goalId = goalId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}