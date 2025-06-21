// BudgetService.java
package com.example.ismoney.service;

import com.example.ismoney.dao.BudgetDAO;
import com.example.ismoney.model.Budget;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class BudgetService {
    private final BudgetDAO budgetDAO;

    public BudgetService() {
        this.budgetDAO = new BudgetDAO();
    }

    public void createBudget(Budget budget) throws SQLException {
        validateBudget(budget);
        budgetDAO.saveBudget(budget);
    }

    public List<Budget> getAllBudgets() throws SQLException {
        return budgetDAO.getAllBudgets();
    }

    public List<Budget> getActiveBudgets() throws SQLException {
        return budgetDAO.getActiveBudgets();
    }

    public List<Budget> getCurrentBudgets() throws SQLException {
        return budgetDAO.getCurrentBudgets();
    }

    public Budget getBudgetById(int id) throws SQLException {
        return budgetDAO.getBudgetById(id);
    }

    public Budget getBudgetByCategory(String category, LocalDate date) throws SQLException {
        return budgetDAO.getBudgetByCategory(category, date);
    }

    public void updateBudget(Budget budget) throws SQLException {
        validateBudget(budget);
        budgetDAO.updateBudget(budget);
    }

    public void deleteBudget(int id) throws SQLException {
        budgetDAO.deleteBudget(id);
    }

    public void deactivateBudget(int id) throws SQLException {
        budgetDAO.deactivateBudget(id);
    }

    // Update spent amount for a budget when a transaction is made
    public void updateBudgetSpent(String category, double amount, LocalDate transactionDate) throws SQLException {
        Budget budget = budgetDAO.getBudgetByCategory(category, transactionDate);
        if (budget != null) {
            budgetDAO.addToBudgetSpent(budget.getId(), amount);
        }
    }

    // Refresh all budget spent amounts based on actual transactions
    public void refreshBudgetSpentAmounts() throws SQLException {
        List<Budget> activeBudgets = getActiveBudgets();

        for (Budget budget : activeBudgets) {
            double actualSpent = budgetDAO.getTotalSpentByCategory(
                    budget.getCategory(),
                    budget.getStartDate(),
                    budget.getEndDate()
            );
            budgetDAO.updateBudgetSpent(budget.getId(), actualSpent);
        }
    }

    // Get budgets that are over limit
    public List<Budget> getOverBudgetBudgets() throws SQLException {
        List<Budget> currentBudgets = getCurrentBudgets();
        return currentBudgets.stream()
                .filter(Budget::isOverBudget)
                .collect(Collectors.toList());
    }

    // Get budgets that are near limit (above specified threshold)
    public List<Budget> getBudgetsNearLimit(double threshold) throws SQLException {
        List<Budget> currentBudgets = getCurrentBudgets();
        return currentBudgets.stream()
                .filter(budget -> budget.isNearLimit(threshold) && !budget.isOverBudget())
                .collect(Collectors.toList());
    }

    // Create monthly budget for a category
    public void createMonthlyBudget(String category, double limitAmount, YearMonth yearMonth) throws SQLException {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Budget budget = new Budget(category, limitAmount, startDate, endDate, "monthly");
        createBudget(budget);
    }

    // Create yearly budget for a category
    public void createYearlyBudget(String category, double limitAmount, int year) throws SQLException {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        Budget budget = new Budget(category, limitAmount, startDate, endDate, "yearly");
        createBudget(budget);
    }

    // Get budget summary
    public BudgetSummary getBudgetSummary() throws SQLException {
        List<Budget> currentBudgets = getCurrentBudgets();

        double totalBudgetAmount = currentBudgets.stream()
                .mapToDouble(Budget::getLimitAmount)
                .sum();

        double totalSpentAmount = currentBudgets.stream()
                .mapToDouble(Budget::getSpentAmount)
                .sum();

        double totalRemainingAmount = currentBudgets.stream()
                .mapToDouble(Budget::getRemainingAmount)
                .sum();

        long overBudgetCount = currentBudgets.stream()
                .filter(Budget::isOverBudget)
                .count();

        long nearLimitCount = currentBudgets.stream()
                .filter(budget -> budget.isNearLimit(80) && !budget.isOverBudget())
                .count();

        return new BudgetSummary(
                totalBudgetAmount,
                totalSpentAmount,
                totalRemainingAmount,
                (int) overBudgetCount,
                (int) nearLimitCount
        );
    }

    // Calculate spending velocity (how fast money is being spent)
    public double calculateSpendingVelocity(Budget budget) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = budget.getStartDate();

        if (today.isBefore(startDate)) {
            return 0; // Budget hasn't started yet
        }

        long daysPassed = startDate.until(today).getDays() + 1; // +1 to include today
        if (daysPassed <= 0) return 0;

        return budget.getSpentAmount() / daysPassed;
    }

    // Predict when budget will be exhausted based on current spending velocity
    public LocalDate predictBudgetExhaustionDate(Budget budget) {
        double velocity = calculateSpendingVelocity(budget);
        if (velocity <= 0) return null;

        double remainingAmount = budget.getRemainingAmount();
        if (remainingAmount <= 0) return LocalDate.now(); // Already exhausted

        long daysToExhaustion = (long) Math.ceil(remainingAmount / velocity);
        return LocalDate.now().plusDays(daysToExhaustion);
    }

    private void validateBudget(Budget budget) {
        if (budget.getCategory() == null || budget.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Budget category cannot be empty");
        }

        if (budget.getLimitAmount() <= 0) {
            throw new IllegalArgumentException("Budget limit amount must be positive");
        }

        if (budget.getSpentAmount() < 0) {
            throw new IllegalArgumentException("Budget spent amount cannot be negative");
        }

        if (budget.getStartDate() == null || budget.getEndDate() == null) {
            throw new IllegalArgumentException("Budget start date and end date cannot be null");
        }

        if (budget.getStartDate().isAfter(budget.getEndDate())) {
            throw new IllegalArgumentException("Budget start date cannot be after end date");
        }

        if (budget.getPeriod() == null || budget.getPeriod().trim().isEmpty()) {
            throw new IllegalArgumentException("Budget period cannot be empty");
        }
    }

    // Inner class for budget summary
    public static class BudgetSummary {
        private final double totalBudgetAmount;
        private final double totalSpentAmount;
        private final double totalRemainingAmount;
        private final int overBudgetCount;
        private final int nearLimitCount;

        public BudgetSummary(double totalBudgetAmount, double totalSpentAmount,
                             double totalRemainingAmount, int overBudgetCount, int nearLimitCount) {
            this.totalBudgetAmount = totalBudgetAmount;
            this.totalSpentAmount = totalSpentAmount;
            this.totalRemainingAmount = totalRemainingAmount;
            this.overBudgetCount = overBudgetCount;
            this.nearLimitCount = nearLimitCount;
        }

        // Getters
        public double getTotalBudgetAmount() { return totalBudgetAmount; }
        public double getTotalSpentAmount() { return totalSpentAmount; }
        public double getTotalRemainingAmount() { return totalRemainingAmount; }
        public int getOverBudgetCount() { return overBudgetCount; }
        public int getNearLimitCount() { return nearLimitCount; }

        public double getOverallUsagePercentage() {
            if (totalBudgetAmount == 0) return 0;
            return (totalSpentAmount / totalBudgetAmount) * 100;
        }
    }
}