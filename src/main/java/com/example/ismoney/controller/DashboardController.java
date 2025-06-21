package com.example.ismoney.controller;

import com.example.ismoney.dao.*;
import com.example.ismoney.model.SavingGoal;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import com.example.ismoney.model.User;
import com.example.ismoney.util.SceneSwitcher;
import com.example.ismoney.util.UserSession;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DashboardController {

    @FXML private Button transactionButton;
    @FXML private Button GoalsListButton;
    @FXML private Button budgetButton;
    @FXML private Button logOutBtn;

    @FXML private TextField totalIncomeField;
    @FXML private TextField totalExpenseField;
    @FXML private TextField totalBalanceField;
    @FXML private DatePicker filterDatePicker;

    @FXML private TableView<ActivityLog> activityLogTable;
    @FXML private TableColumn<ActivityLog, String> dateColumn;
    @FXML private TableColumn<ActivityLog, String> typeColumn;
    @FXML private TableColumn<ActivityLog, String> descriptionColumn;
    @FXML private TableColumn<ActivityLog, String> amountColumn;

    private TransactionDAO transactionDAO;
    private SavingGoalDAO savingGoalDAO;
    private CategoryDAO categoryDAO;
    private UserDAOImpl userDAO;
    private Integer currentUserId;
    private User currentUser;
    private Map<Integer, String> categoryCache = new HashMap<>();

    private Timeline autoRefreshTimeline;
    private static final int REFRESH_INTERVAL_SECONDS = 30;
    private LocalDateTime lastRefreshTime;

    @FXML
    public void initialize() {
        try {
            transactionDAO = new TransactionDAO();
            savingGoalDAO = new SavingGoalDAO();
            categoryDAO = new CategoryDAO();
            userDAO = new UserDAOImpl();

            if (!initializeCurrentUser()) {
                showAlert("Error", "Tidak dapat mengidentifikasi user yang sedang login. Silakan login kembali.");
                handleLogoutButton();
                return;
            }

            loadCategoriesCache();
            setupActivityLogTable();
            setupDatePicker();
            setupAutoRefresh();

            refreshDashboard();

            System.out.println("Dashboard initialized for user: " + currentUser.getUsername() + " (ID: " + currentUserId + ")");

        } catch (Exception e) {
            showAlert("Kesalahan", "Gagal menginisialisasi dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean initializeCurrentUser() {
        try {
            currentUserId = UserSession.getCurrentUserId();
            if (currentUserId != null) {
                currentUser = userDAO.getUserById(currentUserId);
                if (currentUser != null) {
                    System.out.println("User loaded from session: " + currentUser.getUsername());
                    return true;
                }
            }

            System.out.println("No valid user session found. Redirecting to login.");
            return false;

        } catch (Exception e) {
            System.err.println("Error initializing current user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void setupAutoRefresh() {
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> {
                    Platform.runLater(this::refreshDashboard);
                })
        );
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();

        System.out.println("Auto refresh enabled for user " + currentUserId + " - refreshing every " + REFRESH_INTERVAL_SECONDS + " seconds");
    }

    private void refreshDashboard() {
        try {
            if (currentUserId == null || !UserSession.isSessionValid()) {
                System.out.println("Invalid session detected. Logging out...");
                handleLogoutButton();
                return;
            }

            System.out.println("Refreshing dashboard data for user ID: " + currentUserId);

            loadCategoriesCache();
            loadFinancialSummary();
            loadActivityLog();

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
            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }

            refreshDashboard();

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

            List<Transaction> monthlyTransactions = transactionDAO.getTransactionsByUserIdAndMonth(
                    currentUserId, selectedDate.getYear(), selectedDate.getMonthValue());

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
                totalBalanceField.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                totalBalanceField.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }

        } catch (Exception e) {
            System.err.println("Error loading financial summary: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadActivityLog() {
        try {
            ObservableList<ActivityLog> activityLogs = FXCollections.observableArrayList();

            List<Transaction> recentTransactions = transactionDAO.getRecentTransactionsByUserId(currentUserId, 8);
            for (Transaction transaction : recentTransactions) {
                if (!Objects.equals(transaction.getUserId(), currentUserId)) {
                    System.err.println("Warning: Skipping transaction " + transaction.getTransactionId() +
                            " - does not belong to current user " + currentUserId);
                    continue;
                }

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
                    if (!Objects.equals(goal.getUserId(), currentUserId)) {
                        System.err.println("Warning: Skipping saving goal " + goal.getGoalId() +
                                " - does not belong to current user " + currentUserId);
                        continue;
                    }

                    if (goal.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
                        double progressPercentage = 0.0;
                        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                            progressPercentage = goal.getCurrentAmount()
                                    .divide(goal.getTargetAmount(), 4, BigDecimal.ROUND_HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue();
                        }

                        String goalDescription = String.format("Goal: %s (%.1f%% dari target Rp %,.0f)",
                                goal.getGoalName(),
                                progressPercentage,
                                goal.getTargetAmount().doubleValue());

                        String statusIndicator = "";
                        switch (goal.getStatus()) {
                            case "ACTIVE":
                                statusIndicator = " [Aktif]";
                                break;
                            case "COMPLETED":
                                statusIndicator = " [Selesai]";
                                break;
                            case "PAUSED":
                                statusIndicator = " [Dijeda]";
                                break;
                            case "CANCELLED":
                                statusIndicator = " [Dibatal]";
                                break;
                        }
                        goalDescription += statusIndicator;

                        ActivityLog log = new ActivityLog(
                                goal.getCreatedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                "TABUNGAN",
                                goalDescription,
                                "Rp " + String.format("%,.0f", goal.getCurrentAmount().doubleValue())
                        );
                        activityLogs.add(log);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading saving goals for activity log: " + e.getMessage());
                e.printStackTrace();
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

            final ObservableList<ActivityLog> finalActivityLogs;
            if (activityLogs.size() > 15) {
                finalActivityLogs = FXCollections.observableArrayList(activityLogs.subList(0, 15));
            } else {
                finalActivityLogs = activityLogs;
            }

            Platform.runLater(() -> {
                activityLogTable.setItems(finalActivityLogs);
            });

            System.out.println("Loaded " + finalActivityLogs.size() + " activity logs for user " + currentUserId +
                    " (Transactions: " + recentTransactions.size() + ", Goals: " +
                    (finalActivityLogs.size() - recentTransactions.size()) + ")");

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
        try {
            if (autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            }

            UserSession.clearSession();

            // Clear current user data
            currentUserId = null;
            currentUser = null;
            categoryCache.clear();

            System.out.println("User logged out successfully");

            Stage currentStage = (Stage) logOutBtn.getScene().getWindow();
            SceneSwitcher.logout(currentStage, "/com/example/ismoney/Login.fxml");

        } catch (Exception e) {
            System.err.println("Error during logout: " + e.getMessage());
            e.printStackTrace();
        }
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