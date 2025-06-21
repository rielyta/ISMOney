// SavingGoalService.java
package com.example.ismoney.service;

import com.example.ismoney.dao.SavingGoalDAO;
import com.example.ismoney.model.SavingGoal;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class SavingGoalService {
    private final SavingGoalDAO savingGoalDAO;

    public SavingGoalService() {
        this.savingGoalDAO = new SavingGoalDAO();
    }

    public void createSavingGoal(SavingGoal savingGoal) throws SQLException {
        validateSavingGoal(savingGoal);
        savingGoalDAO.addSavingGoal(savingGoal);
    }

    public List<SavingGoal> getAllSavingGoals() throws SQLException {
        return savingGoalDAO.getAllSavingGoals();
    }

    public List<SavingGoal> getActiveSavingGoals() throws SQLException {
        return savingGoalDAO.getActiveSavingGoals();
    }

    public SavingGoal getSavingGoalById(int id) throws SQLException {
        return savingGoalDAO.getSavingGoalById(id);
    }

    public void updateSavingGoal(SavingGoal savingGoal) throws SQLException {
        validateSavingGoal(savingGoal);
        savingGoalDAO.updateSavingGoal(savingGoal);
    }

    public void addToSavingGoal(int goalId, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        savingGoalDAO.addSavingToGoal(goalId, BigDecimal.valueOf(amount));
    }

    public void deleteSavingGoal(int id) throws SQLException {
        savingGoalDAO.deleteSavingGoal(id);
    }

    public void completeSavingGoal(int goalId) throws SQLException {
        savingGoalDAO.updateGoalStatusBasedOnProgress(goalId, "COMPLETED");
    }

    // Calculate how much needs to be saved per day to reach the goal
    public BigDecimal calculateDailySavingRequired(SavingGoal goal) {
        if (goal.getTargetDate().isBefore(LocalDate.now())) {
            return BigDecimal.ZERO; // Goal date has passed
        }

        long daysRemaining = LocalDate.now().until(goal.getTargetDate(), ChronoUnit.DAYS);
        if (daysRemaining <= 0) {
            return goal.getRemainingAmount(); // Need to save everything today
        }

        return goal.getRemainingAmount().divide(BigDecimal.valueOf(daysRemaining), RoundingMode.HALF_UP);
    }

    // Calculate how much needs to be saved per month to reach the goal
    public BigDecimal calculateMonthlySavingRequired(SavingGoal goal) {
        if (goal.getTargetDate().isBefore(LocalDate.now())) {
            return BigDecimal.ZERO; // Goal date has passed
        }

        long monthsRemaining = LocalDate.now().until(goal.getTargetDate()).toTotalMonths();
        if (monthsRemaining <= 0) {
            return goal.getRemainingAmount(); // Need to save everything this month
        }

        return goal.getRemainingAmount().divide(BigDecimal.valueOf(monthsRemaining), RoundingMode.HALF_UP);
    }

    // Get goals that are due soon (within specified days)
    public List<SavingGoal> getGoalsDueSoon(int days) throws SQLException {
        List<SavingGoal> allGoals = getActiveSavingGoals();
        LocalDate cutoffDate = LocalDate.now().plusDays(days);

        return allGoals.stream()
                .filter(goal -> goal.getTargetDate().isBefore(cutoffDate) || goal.getTargetDate().isEqual(cutoffDate))
                .filter(goal -> !goal.isCompleted())
                .toList();
    }

    // Get goals that are overdue
    public List<SavingGoal> getOverdueGoals() throws SQLException {
        List<SavingGoal> allGoals = getActiveSavingGoals();
        LocalDate today = LocalDate.now();

        return allGoals.stream()
                .filter(goal -> goal.getTargetDate().isBefore(today))
                .filter(goal -> !goal.isCompleted())
                .toList();
    }

    // Validate goal input before insert/update
    private void validateSavingGoal(SavingGoal savingGoal) {
        if (savingGoal.getGoalName() == null || savingGoal.getGoalName().trim().isEmpty()) {
            throw new IllegalArgumentException("Saving goal name cannot be empty");
        }

        if (savingGoal.getTargetAmount() == null || savingGoal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target amount must be positive");
        }

        if (savingGoal.getCurrentAmount() == null || savingGoal.getCurrentAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current amount cannot be negative");
        }

        if (savingGoal.getTargetDate() == null || savingGoal.getTargetDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Target date must be today or in the future");
        }
    }
}