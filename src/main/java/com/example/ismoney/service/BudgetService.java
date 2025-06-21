package com.example.ismoney.service;

import com.example.ismoney.dao.BudgetDAO;
import com.example.ismoney.model.Budget;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class BudgetService {
    private final BudgetDAO budgetDAO;

    public BudgetService() {
        this.budgetDAO = new BudgetDAO();
    }

    // Save new budget
    public void saveBudget(Budget budget) throws SQLException {
        // Validate budget before saving
        validateBudget(budget);

        // Check for overlapping budgets in same category
        Budget existingBudget = budgetDAO.getBudgetByCategory(budget.getCategory(), LocalDate.now());
        if (existingBudget != null && existingBudget.isActive()) {
            throw new SQLException("Budget aktif untuk kategori '" + budget.getCategory() + "' sudah ada.");
        }

        budgetDAO.saveBudget(budget);
    }

    // Update existing budget
    public void updateBudget(Budget budget) throws SQLException {
        validateBudget(budget);
        budgetDAO.updateBudget(budget);
    }

    // Get all budgets
    public List<Budget> getAllBudgets() throws SQLException {
        return budgetDAO.getAllBudgets();
    }

    // Get active budgets only
    public List<Budget> getActiveBudgets() throws SQLException {
        return budgetDAO.getActiveBudgets();
    }

    // Get current budgets (active and within date range)
    public List<Budget> getCurrentBudgets() throws SQLException {
        return budgetDAO.getCurrentBudgets();
    }

    // Get budget by ID
    public Budget getBudgetById(int id) throws SQLException {
        return budgetDAO.getBudgetById(id);
    }

    // Get budget by category for specific date
    public Budget getBudgetByCategory(String category, LocalDate date) throws SQLException {
        return budgetDAO.getBudgetByCategory(category, date);
    }

    // Delete budget
    public void deleteBudget(int id) throws SQLException {
        Budget budget = budgetDAO.getBudgetById(id);
        if (budget == null) {
            throw new SQLException("Budget tidak ditemukan.");
        }
        budgetDAO.deleteBudget(id);
    }

    // Deactivate budget instead of deleting
    public void deactivateBudget(int id) throws SQLException {
        budgetDAO.deactivateBudget(id);
    }

    // Update spent amount for a budget
    public void updateBudgetSpent(int budgetId, double spentAmount) throws SQLException {
        budgetDAO.updateBudgetSpent(budgetId, spentAmount);
    }

    // Add to spent amount (for when new transaction is added)
    public void addToBudgetSpent(int budgetId, double amount) throws SQLException {
        budgetDAO.addToBudgetSpent(budgetId, amount);
    }

    // Refresh budget spent amounts based on actual transactions
    public void refreshBudgetSpentAmounts() throws SQLException {
        List<Budget> currentBudgets = getCurrentBudgets();

        for (Budget budget : currentBudgets) {
            double actualSpent = budgetDAO.getTotalSpentByCategory(
                    budget.getCategory(),
                    budget.getStartDate(),
                    budget.getEndDate()
            );

            if (actualSpent != budget.getSpentAmount()) {
                budgetDAO.updateBudgetSpent(budget.getId(), actualSpent);
            }
        }
    }

    // Get budget summary for dashboard
    public BudgetSummary getBudgetSummary() throws SQLException {
        List<Budget> currentBudgets = getCurrentBudgets();

        double totalLimit = currentBudgets.stream()
                .mapToDouble(Budget::getLimitAmount)
                .sum();

        double totalSpent = currentBudgets.stream()
                .mapToDouble(Budget::getSpentAmount)
                .sum();

        long overBudgetCount = currentBudgets.stream()
                .filter(Budget::isOverBudget)
                .count();

        long nearLimitCount = currentBudgets.stream()
                .filter(b -> !b.isOverBudget() && b.isNearLimit(80))
                .count();

        return new BudgetSummary(
                totalLimit,
                totalSpent,
                (int) overBudgetCount,
                (int) nearLimitCount,
                currentBudgets.size()
        );
    }

    // Get budgets that are over limit
    public List<Budget> getOverBudgetAlerts() throws SQLException {
        return getCurrentBudgets().stream()
                .filter(Budget::isOverBudget)
                .collect(Collectors.toList());
    }

    // Get budgets near limit (80% or more)
    public List<Budget> getNearLimitAlerts() throws SQLException {
        return getCurrentBudgets().stream()
                .filter(b -> !b.isOverBudget() && b.isNearLimit(80))
                .collect(Collectors.toList());
    }

    // Get budgets by period
    public List<Budget> getBudgetsByPeriod(String period) throws SQLException {
        return getAllBudgets().stream()
                .filter(b -> b.getPeriod().equalsIgnoreCase(period))
                .collect(Collectors.toList());
    }

    // Validate budget data
    private void validateBudget(Budget budget) throws SQLException {
        if (budget.getCategory() == null || budget.getCategory().trim().isEmpty()) {
            throw new SQLException("Kategori budget tidak boleh kosong.");
        }

        if (budget.getLimitAmount() <= 0) {
            throw new SQLException("Batas anggaran harus lebih dari 0.");
        }

        if (budget.getStartDate() == null) {
            throw new SQLException("Tanggal mulai tidak boleh kosong.");
        }

        if (budget.getEndDate() == null) {
            throw new SQLException("Tanggal berakhir tidak boleh kosong.");
        }

        if (budget.getEndDate().isBefore(budget.getStartDate())) {
            throw new SQLException("Tanggal berakhir tidak boleh lebih awal dari tanggal mulai.");
        }

        if (budget.getPeriod() == null || budget.getPeriod().trim().isEmpty()) {
            throw new SQLException("Periode budget tidak boleh kosong.");
        }

        // Validate period values
        String period = budget.getPeriod().toLowerCase();
        if (!period.equals("monthly") && !period.equals("weekly") && !period.equals("yearly")) {
            throw new SQLException("Periode budget harus 'monthly', 'weekly', atau 'yearly'.");
        }
    }

    // Inner class for budget summary
    public static class BudgetSummary {
        public final double totalLimit;
        public final double totalSpent;
        public final double remainingAmount;
        public final double usagePercentage;
        public final int overBudgetCount;
        public final int nearLimitCount;
        public final int totalBudgetsCount;

        public BudgetSummary(double totalLimit, double totalSpent, int overBudgetCount,
                             int nearLimitCount, int totalBudgetsCount) {
            this.totalLimit = totalLimit;
            this.totalSpent = totalSpent;
            this.remainingAmount = totalLimit - totalSpent;
            this.usagePercentage = totalLimit > 0 ? (totalSpent / totalLimit) * 100 : 0;
            this.overBudgetCount = overBudgetCount;
            this.nearLimitCount = nearLimitCount;
            this.totalBudgetsCount = totalBudgetsCount;
        }
    }
}