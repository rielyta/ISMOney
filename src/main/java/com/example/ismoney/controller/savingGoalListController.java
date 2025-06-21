package com.example.ismoney.controller;

import com.example.ismoney.dao.SavingGoalDAO;
import com.example.ismoney.model.SavingGoal;
import com.example.ismoney.service.SavingGoalService;
import com.example.ismoney.util.SceneSwitcher;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ResourceBundle;

public class savingGoalListController implements Initializable {

    @FXML private TableView<SavingGoal> goalTableView;
    @FXML private TableColumn<SavingGoal, String> goalNameColumn;
    @FXML private TableColumn<SavingGoal, BigDecimal> targetAmountColumn;
    @FXML private TableColumn<SavingGoal, BigDecimal> currentAmountColumn;
    @FXML private TableColumn<SavingGoal, String> targetDateColumn;
    @FXML private TableColumn<SavingGoal, String> statusColumn;
    @FXML private TableColumn<SavingGoal, String> progressColumn;

    @FXML private TextField addSavingAmountField;
    @FXML private Button addSavingButton;
    @FXML private Button refreshButton;
    @FXML private Button GoalFormButton;
    @FXML private Button backButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;

    private SavingGoalDAO savingGoalDAO;
    private SavingGoalService savingGoalService;
    private ObservableList<SavingGoal> allGoals;
    private FilteredList<SavingGoal> filteredGoals;
    private Integer currentUserId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("SavingGoalListController initialized!");

        try {
            savingGoalDAO = new SavingGoalDAO();
            savingGoalService = new SavingGoalService();

            // Get current user ID using the same pattern as TransactionListController
            currentUserId = getCurrentLoggedInUserId();
            System.out.println("Using user ID for saving goals: " + currentUserId);

            allGoals = FXCollections.observableArrayList();
            filteredGoals = new FilteredList<>(allGoals, p -> true);

            setupTableColumns();
            setupEventHandlers();
            setupSearchFunctionality();
            loadGoals();

            System.out.println("SavingGoalListController setup completed!");
        } catch (Exception e) {
            System.err.println("Error initializing SavingGoalListController: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Kesalahan", "Gagal menginisialisasi controller: " + e.getMessage());
        }
    }

    private Integer getCurrentLoggedInUserId() {
        try {
            Integer latestUserId = getLatestUserId();
            if (latestUserId != null) {
                System.out.println("Using latest user ID: " + latestUserId);
                return latestUserId;
            }

            Integer existingUserId = getFirstExistingUserId();
            if (existingUserId != null) {
                System.out.println("Using first existing user ID: " + existingUserId);
                return existingUserId;
            }

            System.out.println("No users found, using default ID: 1");
            return 1;
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

    private void setupSearchFunctionality() {
        // Set the filtered list as the table's items
        goalTableView.setItems(filteredGoals);

        // buat real-time search saat mengetik
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterGoals(newValue);
        });
    }

    private void filterGoals(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // Show all goals if search text is empty
            filteredGoals.setPredicate(goal -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase().trim();
            filteredGoals.setPredicate(goal -> {
                return goal.getGoalName().toLowerCase().contains(lowerCaseFilter);
            });
        }
    }

    @FXML
    private void handleFilter() {
        String searchText = searchField.getText();
        filterGoals(searchText);

        int totalGoals = allGoals.size();
        int filteredCount = filteredGoals.size();

        if (!searchText.trim().isEmpty()) {
            if (filteredCount == 0) {
                showAlert(Alert.AlertType.INFORMATION, "Pencarian",
                        "Tidak ditemukan target dengan nama: \"" + searchText + "\"");
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Pencarian",
                        "Ditemukan " + filteredCount + " dari " + totalGoals + " target");
            }
        }
    }

    private void setupTableColumns() {
        goalNameColumn.setCellValueFactory(new PropertyValueFactory<>("goalName"));
        targetAmountColumn.setCellValueFactory(new PropertyValueFactory<>("targetAmount"));
        currentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("currentAmount"));
        targetDateColumn.setCellValueFactory(new PropertyValueFactory<>("targetDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Set alignment untuk kolom progress
        progressColumn.setStyle("-fx-alignment: CENTER;");

        // Custom cell untuk progress column
        progressColumn.setCellValueFactory(cellData -> {
            return new javafx.beans.property.SimpleStringProperty("");
        });

        // Custom cell untuk status dgn warna
        statusColumn.setCellFactory(column -> new TableCell<SavingGoal, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    SavingGoal goal = getTableView().getItems().get(getIndex());
                    String progressStatus = goal.getProgressStatus();

                    setText(status);

                    switch (progressStatus) {
                        case "COMPLETED":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "OVERDUE":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: blue;");
                            break;
                    }
                }
            }
        });

        progressColumn.setCellFactory(column -> new TableCell<SavingGoal, String>() {
            @Override
            protected void updateItem(String progress, boolean empty) {
                super.updateItem(progress, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    SavingGoal goal = getTableView().getItems().get(getIndex());

                    // hitung persentase progress
                    double progressPercentage = calculateProgressPercentage(goal);

                    ProgressBar styledProgressBar = createStyledProgressBar(progressPercentage, goal);
                    setGraphic(styledProgressBar);
                    setText(null);
                }
            }
        });

        formatCurrencyColumns();
    }

    private void formatCurrencyColumns() {
        targetAmountColumn.setCellFactory(column -> new TableCell<SavingGoal, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                } else {
                    setText("Rp " + String.format("%,.0f", amount.doubleValue()));
                }
            }
        });

        currentAmountColumn.setCellFactory(column -> new TableCell<SavingGoal, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                } else {
                    setText("Rp " + String.format("%,.0f", amount.doubleValue()));
                    setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                }
            }
        });
    }

    // Helper method untuk menghitung persentase progress
    private double calculateProgressPercentage(SavingGoal goal) {
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            return goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        }
        return 0.0;
    }

    private ProgressBar createStyledProgressBar(double progressPercentage, SavingGoal goal) {
        ProgressBar progressBar = new ProgressBar();

        // Hitung persentase progress berdasarkan currentAmount dan targetAmount
        double actualProgressPercentage = 0.0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            actualProgressPercentage = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        }

        // mastiin progress tidak melebihi 100%
        double progressValue = Math.min(actualProgressPercentage / 100.0, 1.0);

        progressBar.setProgress(progressValue);
        progressBar.setPrefWidth(120);
        progressBar.setMaxWidth(120);
        progressBar.setPrefHeight(18);

        String barStyle;
        if (goal.isCompleted()) {
            barStyle = "-fx-accent: #4CAF50; -fx-control-inner-background: #e8f5e8; -fx-padding: 2px;";
        } else if (goal.isOverdue()) {
            barStyle = "-fx-accent: #F44336; -fx-control-inner-background: #fdeaea; -fx-padding: 2px;";
        } else {
            barStyle = "-fx-accent: #2196F3; -fx-control-inner-background: #e3f2fd; -fx-padding: 2px;";
        }

        progressBar.setStyle(barStyle);

        Tooltip tooltip = new Tooltip(String.format(
                "%s\nCurrent: Rp %,.0f\nTarget: Rp %,.0f\nProgress: %.1f%%\nRemaining: Rp %,.0f",
                goal.getGoalName(),
                goal.getCurrentAmount().doubleValue(),
                goal.getTargetAmount().doubleValue(),
                actualProgressPercentage,
                goal.getRemainingAmount().doubleValue()
        ));
        tooltip.setShowDelay(javafx.util.Duration.millis(300));
        Tooltip.install(progressBar, tooltip);

        return progressBar;
    }

    private void setupEventHandlers() {
        addSavingButton.setOnAction(event -> handleAddSaving());
        refreshButton.setOnAction(event -> loadGoals());
        GoalFormButton.setOnAction(event -> handleGoalFormButton());
        searchButton.setOnAction(event -> handleFilter());

        // Double click untuk edit
        goalTableView.setRowFactory(tv -> {
            TableRow<SavingGoal> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showGoalDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void handleAddSaving() {
        SavingGoal selectedGoal = goalTableView.getSelectionModel().getSelectedItem();

        if (selectedGoal == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih goal terlebih dahulu");
            return;
        }

        if (addSavingAmountField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Masukkan jumlah tabungan");
            addSavingAmountField.requestFocus();
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(addSavingAmountField.getText().trim());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert(Alert.AlertType.ERROR, "Error", "Jumlah harus lebih besar dari 0");
                return;
            }

            // Using user-specific method from SavingGoalDAO
            boolean success = savingGoalDAO.addSavingToGoal(selectedGoal.getGoalId(), amount, currentUserId);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses",
                        "Tabungan berhasil ditambahkan ke " + selectedGoal.getGoalName());
                addSavingAmountField.clear();
                loadGoals(); // Refresh data

                // Ambil data goal yang sudah diupdate untuk ngecek status
                SavingGoal updatedGoal = savingGoalDAO.getSavingGoalById(selectedGoal.getGoalId(), currentUserId);
                if (updatedGoal != null && updatedGoal.isCompleted()) {
                    savingGoalDAO.updateGoalStatusBasedOnProgress(selectedGoal.getGoalId(), "COMPLETED", currentUserId);
                    showAlert(Alert.AlertType.INFORMATION, "Selamat!",
                            "Target " + selectedGoal.getGoalName() + " telah tercapai! 🎉");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menambahkan tabungan");
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Jumlah harus berupa angka");
            addSavingAmountField.requestFocus();
        } catch (Exception e) {
            System.err.println("Error adding saving: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleGoalFormButton() {
        try {
            SceneSwitcher.switchTo("savingGoals/savingGoalForm.fxml", (Stage) GoalFormButton.getScene().getWindow());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal membuka form saving goals: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadGoals() {
        try {
            System.out.println("=== DEBUG: Loading saving goals ===");
            System.out.println("Loading saving goals for user ID: " + currentUserId);

            // Using user-specific method from SavingGoalDAO
            List<SavingGoal> goals = savingGoalDAO.getSavingGoalsByUserId(currentUserId);
            allGoals.clear();
            allGoals.addAll(goals);

            System.out.println("Loaded " + goals.size() + " saving goals");

            if (goals.isEmpty()) {
                System.out.println("No saving goals found for user ID " + currentUserId);
            } else {
                for (int i = 0; i < Math.min(3, goals.size()); i++) {
                    SavingGoal g = goals.get(i);
                    System.out.println("Goal " + (i + 1) + ": ID=" + g.getGoalId() +
                            ", Name=" + g.getGoalName() +
                            ", Target=" + g.getTargetAmount() +
                            ", Current=" + g.getCurrentAmount() +
                            ", Status=" + g.getStatus());
                }
                if (goals.size() > 3) {
                    System.out.println("... and " + (goals.size() - 3) + " more goals");
                }
            }

            // Reset search filter after loading new data
            String currentSearchText = searchField.getText();
            if (currentSearchText != null && !currentSearchText.trim().isEmpty()) {
                filterGoals(currentSearchText);
            }

        } catch (Exception e) {
            System.err.println("Error loading saving goals: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat data: " + e.getMessage());
        }
    }

    private void showGoalDetails(SavingGoal goal) {
        String details = String.format(
                "Goal: %s\n" +
                        "Target: Rp %,.2f\n" +
                        "Current: Rp %,.2f\n" +
                        "Progress: %.1f%%\n" +
                        "Remaining: Rp %,.2f\n" +
                        "Target Date: %s\n" +
                        "Status: %s\n" +
                        "Progress Status: %s",
                goal.getGoalName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getProgressPercentage(),
                goal.getRemainingAmount(),
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getProgressStatus()
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detail Goal");
        alert.setHeaderText(goal.getGoalName());
        alert.setContentText(details);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void backTo() {
        SceneSwitcher.switchTo("Dashboard.fxml", (Stage) backButton.getScene().getWindow());
    }
}