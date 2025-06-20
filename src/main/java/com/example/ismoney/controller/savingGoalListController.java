package com.example.ismoney.controller;

import com.example.ismoney.dao.SavingGoalDAO;
import com.example.ismoney.model.savingGoal;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class savingGoalListController implements Initializable {

    @FXML private TableView<savingGoal> goalTableView;
    @FXML private TableColumn<savingGoal, String> goalNameColumn;
    @FXML private TableColumn<savingGoal, BigDecimal> targetAmountColumn;
    @FXML private TableColumn<savingGoal, BigDecimal> currentAmountColumn;
    @FXML private TableColumn<savingGoal, LocalDate> targetDateColumn;
    @FXML private TableColumn<savingGoal, String> statusColumn;
    @FXML private TableColumn<savingGoal, String> progressColumn;

    @FXML private TextField addSavingAmountField;
    @FXML private Button addSavingButton;
    @FXML private Button refreshButton;

    private SavingGoalDAO savingGoalDAO;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        savingGoalDAO = new SavingGoalDAO();

        setupTableColumns();
        setupEventHandlers();
        loadGoals();
    }

    private void setupTableColumns() {
        goalNameColumn.setCellValueFactory(new PropertyValueFactory<>("goalName"));
        targetAmountColumn.setCellValueFactory(new PropertyValueFactory<>("targetAmount"));
        currentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("currentAmount"));
        targetDateColumn.setCellValueFactory(new PropertyValueFactory<>("targetDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // custom cell untuk nampilin progres
        progressColumn.setCellValueFactory(cellData -> {
            savingGoal goal = cellData.getValue();
            String progress = String.format("%.1f%%", goal.getProgressPercentage());
            return new javafx.beans.property.SimpleStringProperty(progress);
        });

        // custom cell untuk nampilin status bewarna
        statusColumn.setCellFactory(column -> new TableCell<savingGoal, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    savingGoal goal = getTableView().getItems().get(getIndex());
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

        // custom cell untuk progress bar
        progressColumn.setCellFactory(column -> new TableCell<savingGoal, String>() {
            private final ProgressBar progressBar = new ProgressBar();

            @Override
            protected void updateItem(String progress, boolean empty) {
                super.updateItem(progress, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    savingGoal goal = getTableView().getItems().get(getIndex());
                    double progressValue = goal.getProgressPercentage() / 100.0;

                    progressBar.setProgress(progressValue);
                    progressBar.setPrefWidth(100);

                    // Set warna progress bar
                    if (goal.isCompleted()) {
                        progressBar.setStyle("-fx-accent: green;");
                    } else if (goal.isOverdue()) {
                        progressBar.setStyle("-fx-accent: red;");
                    } else {
                        progressBar.setStyle("-fx-accent: blue;");
                    }

                    setGraphic(progressBar);
                }
            }
        });
    }

    private void setupEventHandlers() {
        addSavingButton.setOnAction(event -> handleAddSaving());
        refreshButton.setOnAction(event -> loadGoals());

        // Double click untuk edit
        goalTableView.setRowFactory(tv -> {
            TableRow<savingGoal> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    // TODO: Open edit form
                    showGoalDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void handleAddSaving() {
        savingGoal selectedGoal = goalTableView.getSelectionModel().getSelectedItem();

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

            boolean success = savingGoalDAO.addSavingToGoal(selectedGoal.getGoalId(), amount);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses",
                        "Tabungan berhasil ditambahkan ke " + selectedGoal.getGoalName());
                addSavingAmountField.clear();
                loadGoals(); // Refresh data

                // Update status otomatis
                savingGoalDAO.updateGoalStatusBasedOnProgress();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menambahkan tabungan");
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Jumlah harus berupa angka");
            addSavingAmountField.requestFocus();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error: " + e.getMessage());
        }
    }

    private void loadGoals() {
        try {
            List<savingGoal> goals = savingGoalDAO.getAllSavingGoals();
            goalTableView.getItems().clear();
            goalTableView.getItems().addAll(goals);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Gagal memuat data: " + e.getMessage());
        }
    }

    private void showGoalDetails(savingGoal goal) {
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
}