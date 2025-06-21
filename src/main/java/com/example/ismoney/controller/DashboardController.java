package com.example.ismoney.controller;

import com.example.ismoney.dao.CategoryDAO;
import com.example.ismoney.dao.SavingGoalDAO;
import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.model.SavingGoal;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import com.example.ismoney.util.SceneSwitcher;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private Button transactionButton;
    @FXML private Button GoalsListButton;
    @FXML private Button budgetButton;
    @FXML private Button logOutBtn;

    // FXML fields untuk ringkasan keuangan
    @FXML private TextField totalIncomeField;
    @FXML private TextField totalExpenseField;
    @FXML private TextField totalBalanceField;
    @FXML private DatePicker filterDatePicker;

    // FXML fields untuk log aktivitas
    @FXML private TableView<ActivityLog> activityLogTable;
    @FXML private TableColumn<ActivityLog, String> dateColumn;
    @FXML private TableColumn<ActivityLog, String> typeColumn;
    @FXML private TableColumn<ActivityLog, String> descriptionColumn;
    @FXML private TableColumn<ActivityLog, String> amountColumn;

    private TransactionDAO transactionDAO;
    private SavingGoalDAO savingGoalDAO;
    private CategoryDAO categoryDAO;
    private Integer currentUserId;
    private Map<Integer, String> categoryCache = new HashMap<>();

    // Auto refresh components
    private Timeline autoRefreshTimeline;
    private static final int REFRESH_INTERVAL_SECONDS = 30;
    private LocalDateTime lastRefreshTime;

    @FXML
    public void initialize() {
        try {
            transactionDAO = new TransactionDAO();
            savingGoalDAO = new SavingGoalDAO();
            categoryDAO = new CategoryDAO();

            currentUserId = getCurrentLoggedInUserId();
            loadCategoriesCache();
            setupActivityLogTable();
            setupDatePicker();
            setupAutoRefresh();

            // Initial load
            refreshDashboard();

        } catch (Exception e) {
            showAlert("Kesalahan", "Gagal menginisialisasi dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupAutoRefresh() {
        // Create auto refresh timeline
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> {
                    Platform.runLater(this::refreshDashboard);
                })
        );
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();

        System.out.println("Auto refresh enabled - refreshing every " + REFRESH_INTERVAL_SECONDS + " seconds");
    }

    private void refreshDashboard() {
        try {
            System.out.println("Refreshing dashboard data...");

            // Refresh data
            loadCategoriesCache();
            loadFinancialSummary();
            loadActivityLog();

            // Update last refresh time
            lastRefreshTime = LocalDateTime.now();

            System.out.println("Dashboard refreshed at: " + lastRefreshTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        } catch (Exception e) {
            System.err.println("Error during auto refresh: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupDatePicker() {
        filterDatePicker.setValue(LocalDate.now());
        filterDatePicker.setOnAction(event -> {
            // Stop auto refresh temporarily during manual refresh
            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }

            refreshDashboard();

            // Restart auto refresh
            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.play();
            }
        });
    }

    private void setupActivityLogTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        typeColumn.setCellFactory(column -> new TableCell<ActivityLog, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(type);
                    switch (type) {
                        case "PEMASUKAN":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "PENGELUARAN":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        case "TABUNGAN":
                            setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });

        amountColumn.setCellFactory(column -> new TableCell<ActivityLog, String>() {
            @Override
            protected void updateItem(String amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(amount);
                    ActivityLog log = getTableView().getItems().get(getIndex());
                    if (log != null) {
                        switch (log.getType()) {
                            case "PEMASUKAN":
                                setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                                break;
                            case "PENGELUARAN":
                                setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                                break;
                            case "TABUNGAN":
                                setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                                break;
                        }
                    }
                }
            }
        });
    }

    private void loadFinancialSummary() {
        try {
            LocalDate selectedDate = filterDatePicker.getValue();
            if (selectedDate == null) {
                selectedDate = LocalDate.now();
            }

            System.out.println("Loading financial summary for user ID: " + currentUserId +
                    ", Year: " + selectedDate.getYear() +
                    ", Month: " + selectedDate.getMonthValue());

            List<Transaction> monthlyTransactions = transactionDAO.getTransactionsByUserIdAndMonth(
                    currentUserId, selectedDate.getYear(), selectedDate.getMonthValue());

            System.out.println("Found " + monthlyTransactions.size() + " transactions");

            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpense = BigDecimal.ZERO;

            for (Transaction transaction : monthlyTransactions) {
                if (transaction.getType() == TransactionType.INCOME) {
                    totalIncome = totalIncome.add(transaction.getAmount());
                } else if (transaction.getType() == TransactionType.OUTCOME) {
                    totalExpense = totalExpense.add(transaction.getAmount());
                }
            }

            BigDecimal totalBalance = totalIncome.subtract(totalExpense);

            totalIncomeField.setText("Rp " + String.format("%,.0f", totalIncome.doubleValue()));
            totalExpenseField.setText("Rp " + String.format("%,.0f", totalExpense.doubleValue()));
            totalBalanceField.setText("Rp " + String.format("%,.0f", totalBalance.doubleValue()));

            if (totalBalance.compareTo(BigDecimal.ZERO) >= 0) {
                totalBalanceField.setStyle("-fx-text-fill: green; -fx-font-weight: bold; -fx-border-color: #3498db; -fx-border-radius: 5; -fx-background-color: #f8f9fa;");
            } else {
                totalBalanceField.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-border-color: #3498db; -fx-border-radius: 5; -fx-background-color: #f8f9fa;");
            }

        } catch (Exception e) {
            System.err.println("Error loading financial summary: " + e.getMessage());
            e.printStackTrace();

            totalIncomeField.setText("Rp 0");
            totalExpenseField.setText("Rp 0");
            totalBalanceField.setText("Rp 0");

            showAlert("Error", "Gagal memuat ringkasan keuangan: " + e.getMessage());
        }
    }

    private void loadActivityLog() {
        try {
            ObservableList<ActivityLog> activityLogs = FXCollections.observableArrayList();
            LocalDate selectedDate = filterDatePicker.getValue();
            if (selectedDate == null) {
                selectedDate = LocalDate.now();
            }

            List<Transaction> recentTransactions = transactionDAO.getRecentTransactionsByUserId(currentUserId, 10);
            for (Transaction transaction : recentTransactions) {
                String categoryName = categoryCache.getOrDefault(transaction.getCategoryId(), "Unknown");
                String type = transaction.getType() == TransactionType.INCOME ? "PEMASUKAN" : "PENGELUARAN";
                String description = categoryName;
                if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
                    description += " - " + transaction.getNote();
                }

                ActivityLog log = new ActivityLog(
                        transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        type,
                        description,
                        "Rp " + String.format("%,.0f", transaction.getAmount().doubleValue())
                );
                activityLogs.add(log);
            }

            try {
                List<SavingGoal> recentGoals = savingGoalDAO.getRecentUpdatedGoalsByUserId(currentUserId, 5);
                for (SavingGoal goal : recentGoals) {
                    if (goal.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
                        ActivityLog log = new ActivityLog(
                                goal.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                "TABUNGAN",
                                "Tabungan: " + goal.getGoalName() + " (" +
                                        String.format("%.1f%%", goal.getProgressPercentage()) + ")",
                                "Rp " + String.format("%,.0f", goal.getCurrentAmount().doubleValue())
                        );
                        activityLogs.add(log);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading saving goals for activity log: " + e.getMessage());
            }

            activityLogs.sort((a, b) -> {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate dateA = LocalDate.parse(a.getDate(), formatter);
                    LocalDate dateB = LocalDate.parse(b.getDate(), formatter);
                    return dateB.compareTo(dateA);
                } catch (Exception e) {
                    return b.getDate().compareTo(a.getDate());
                }
            });

            Platform.runLater(() -> {
                activityLogTable.setItems(activityLogs);
            });

        } catch (Exception e) {
            System.err.println("Error loading activity log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCategoriesCache() {
        try {
            categoryCache.clear();
            categoryDAO.getAllCategories().forEach(category ->
                    categoryCache.put(category.getCategoriesId(), category.getName())
            );
        } catch (Exception e) {
            System.err.println("Error loading categories cache: " + e.getMessage());
        }
    }

    private Integer getCurrentLoggedInUserId() {
        try {
            Integer latestUserId = getLatestUserId();
            if (latestUserId != null) {
                return latestUserId;
            }

            Integer existingUserId = getFirstExistingUserId();
            if (existingUserId != null) {
                return existingUserId;
            }

            return 1; // Default user ID
        } catch (Exception e) {
            System.err.println("Error getting current user ID: " + e.getMessage());
            return 1;
        }
    }

    private Integer getLatestUserId() {
        try (Connection conn = com.example.ismoney.database.DatabaseConfig.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users ORDER BY created_at DESC, id DESC LIMIT 1");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            System.err.println("Error getting latest user ID: " + e.getMessage());
        }
        return null;
    }

    private Integer getFirstExistingUserId() {
        try (Connection conn = com.example.ismoney.database.DatabaseConfig.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users ORDER BY id LIMIT 1");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            System.err.println("Error getting existing user ID: " + e.getMessage());
        }
        return null;
    }

    @FXML
    private void handleTransactionButton() {
        try {
            SceneSwitcher.switchTo("Transaction/TransactionList.fxml", (Stage) transactionButton.getScene().getWindow());
        } catch (Exception e) {
            showAlert("Kesalahan", "Terjadi kesalahan tidak terduga: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoalsListButton() {
        try {
            SceneSwitcher.switchTo("savingGoals/savingGoalList.fxml", (Stage) GoalsListButton.getScene().getWindow());
        } catch (Exception e) {
            showAlert("Kesalahan", "Terjadi kesalahan tidak terduga: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBudgetButton() {
        try {
            SceneSwitcher.switchTo("Budget/Budget.fxml", (Stage) budgetButton.getScene().getWindow());

        } catch (Exception e) {
            showAlert("Kesalahan", "Terjadi kesalahan tidak terduga: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogoutButton() {
        // Stop auto refresh before logout
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }

        Stage currentStage = (Stage) logOutBtn.getScene().getWindow();
        SceneSwitcher.logout(currentStage, "/com/example/ismoney/Login.fxml");
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static class ActivityLog {
        private final SimpleStringProperty date;
        private final SimpleStringProperty type;
        private final SimpleStringProperty description;
        private final SimpleStringProperty amount;

        public ActivityLog(String date, String type, String description, String amount) {
            this.date = new SimpleStringProperty(date);
            this.type = new SimpleStringProperty(type);
            this.description = new SimpleStringProperty(description);
            this.amount = new SimpleStringProperty(amount);
        }

        public String getDate() { return date.get(); }
        public void setDate(String date) { this.date.set(date); }

        public String getType() { return type.get(); }
        public void setType(String type) { this.type.set(type); }

        public String getAmount() { return amount.get(); }
        public void setAmount(String amount) { this.amount.set(amount); }
    }
}