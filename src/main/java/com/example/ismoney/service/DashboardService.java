// DashboardService.java
package com.example.ismoney.service;

import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.Budget;
import com.example.ismoney.model.SavingGoal;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DashboardService {
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final SavingGoalService savingGoalService;

    public DashboardService() {
        this.transactionService = new TransactionService();
        this.budgetService = new BudgetService();
        this.savingGoalService = new SavingGoalService();
    }

    // Get dashboard summary for a specific date
    public DashboardSummary getDashboardSummary(LocalDate date) throws SQLException {
        // Get financial data for the date
        double totalIncome = transactionService.getTotalIncomeByDate(date);
        double totalExpense = transactionService.getTotalExpenseByDate(date);
        double netIncome = totalIncome - totalExpense;

        // Get current balance (total of all transactions up to date)
        double currentBalance = calculateCurrentBalance(date);

        // Get budget information
        BudgetService.BudgetSummary budgetSummary = budgetService.getBudgetSummary();

        // Get saving goals information
        List<SavingGoal> activeSavingGoals = savingGoalService.getActiveSavingGoals();
        BigDecimal totalSavingGoals = activeSavingGoals.stream()
                .map(SavingGoal::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSaved = activeSavingGoals.stream()
                .map(SavingGoal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get recent transactions
        List<Transaction> recentTransactions = transactionService.getTransactionsByDate(date);

        return new DashboardSummary(
                totalIncome, totalExpense, netIncome, currentBalance,
                budgetSummary, totalSavingGoals, totalSaved,
                activeSavingGoals.size(), recentTransactions
        );
    }

    // Get monthly dashboard summary
    public MonthlyDashboardSummary getMonthlyDashboardSummary(YearMonth yearMonth) throws SQLException {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Get monthly transactions
        List<Transaction> monthlyTransactions = transactionService.getTransactionsByDateRange(startDate, endDate);

        double monthlyIncome = monthlyTransactions.stream()
                .filter(t -> "income".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double monthlyExpense = monthlyTransactions.stream()
                .filter(t -> "expense".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Get category breakdown
        Map<String, Double> expenseByCategory = monthlyTransactions.stream()
                .filter(t -> "expense".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        Map<String, Double> incomeByCategory = monthlyTransactions.stream()
                .filter(t -> "income".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        // Get budget performance
        List<Budget> monthlyBudgets = budgetService.getCurrentBudgets().stream()
                .filter(b -> "monthly".equals(b.getPeriod()))
                .collect(Collectors.toList());

        return new MonthlyDashboardSummary(
                yearMonth, monthlyIncome, monthlyExpense,
                expenseByCategory, incomeByCategory, monthlyBudgets
        );
    }

    // Calculate current balance (total saldo)
    private double calculateCurrentBalance(LocalDate upToDate) throws SQLException {
        List<Transaction> allTransactions = transactionService.getTransactionsByDateRange(
                LocalDate.of(2020, 1, 1), // Start from a past date
                upToDate
        );

        double balance = 0;
        for (Transaction transaction : allTransactions) {
            if ("income".equals(transaction.getType())) {
                balance += transaction.getAmount();
            } else if ("expense".equals(transaction.getType())) {
                balance -= transaction.getAmount();
            }
        }

        return balance;
    }

    // Get chart data for expenses by category
    public Map<String, Double> getExpenseChartData(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Transaction> transactions = transactionService.getTransactionsByDateRange(startDate, endDate);

        return transactions.stream()
                .filter(t -> "expense".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory() != null ? transaction.getCategory() : "Lainnya",
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    // Get chart data for income vs expense over time
    public Map<LocalDate, DailyFinancialData> getDailyFinancialChartData(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<Transaction> transactions = transactionService.getTransactionsByDateRange(startDate, endDate);
        Map<LocalDate, DailyFinancialData> dailyData = new HashMap<>();

        // Initialize all dates with zero values
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dailyData.put(current, new DailyFinancialData(current, 0, 0));
            current = current.plusDays(1);
        }

        // Populate with actual transaction data
        for (Transaction transaction : transactions) {
            DailyFinancialData data = dailyData.get(transaction.getDate());
            if (data != null) {
                if ("income".equals(transaction.getType())) {
                    data.income += transaction.getAmount();
                } else if ("expense".equals(transaction.getType())) {
                    data.expense += transaction.getAmount();
                }
            }
        }

        return dailyData;
    }

    // Get budget usage percentage data for chart
    public Map<String, Double> getBudgetUsageChartData() throws SQLException {
        List<Budget> currentBudgets = budgetService.getCurrentBudgets();
        Map<String, Double> budgetUsage = new HashMap<>();

        for (Budget budget : currentBudgets) {
            budgetUsage.put(budget.getCategory(), budget.getUsagePercentage());
        }

        return budgetUsage;
    }

    // Inner class for general daily dashboard summary
    public static class DashboardSummary {
        public final double totalIncome;
        public final double totalExpense;
        public final double netIncome;
        public final double currentBalance;
        public final BigDecimal totalSavingGoals; // ← ubah ke BigDecimal
        public final BigDecimal totalSaved;
        public final BudgetService.BudgetSummary budgetSummary;
        public final int activeGoalsCount;
        public final List<Transaction> recentTransactions;

        public DashboardSummary(
                double totalIncome,
                double totalExpense,
                double netIncome,
                double currentBalance,
                BudgetService.BudgetSummary budgetSummary,
                BigDecimal totalSavingGoals,
                BigDecimal totalSaved,
                int activeGoalsCount,
                List<Transaction> recentTransactions
        ) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.netIncome = netIncome;
            this.currentBalance = currentBalance;
            this.budgetSummary = budgetSummary;
            this.totalSavingGoals = totalSavingGoals;
            this.totalSaved = totalSaved;
            this.activeGoalsCount = activeGoalsCount;
            this.recentTransactions = recentTransactions;
        }
    }

    // Inner class for monthly dashboard summary
    public static class MonthlyDashboardSummary {
        public final YearMonth month;
        public final double income;
        public final double expense;
        public final Map<String, Double> expenseByCategory;
        public final Map<String, Double> incomeByCategory;
        public final List<Budget> budgets;

        public MonthlyDashboardSummary(
                YearMonth month,
                double income,
                double expense,
                Map<String, Double> expenseByCategory,
                Map<String, Double> incomeByCategory,
                List<Budget> budgets
        ) {
            this.month = month;
            this.income = income;
            this.expense = expense;
            this.expenseByCategory = expenseByCategory;
            this.incomeByCategory = incomeByCategory;
            this.budgets = budgets;
        }
    }

    // Inner class for daily chart data (used in line graph)
    public static class DailyFinancialData {
        public final LocalDate date;
        public double income;
        public double expense;

        public DailyFinancialData(LocalDate date, double income, double expense) {
            this.date = date;
            this.income = income;
            this.expense = expense;
        }
    }
}
