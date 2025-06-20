package com.example.ismoney.controller;

import com.example.ismoney.dao.SavingGoalDAO;
import com.example.ismoney.model.savingGoal;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class savingGoalFormController implements Initializable {

    @FXML private TextField goalNameField;
    @FXML private TextField targetAmountField;
    @FXML private TextField currentAmountField;
    @FXML private DatePicker targetDatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Button simpanButton;
    @FXML private Button batalButton;

    // Progress display elements
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Label remainingLabel;
    @FXML private Label statusLabel;

    private SavingGoalDAO savingGoalDAO;
    private savingGoal currentGoal;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        savingGoalDAO = new SavingGoalDAO();

        setupStatusComboBox();
        setupEventHandlers();
        setupDefaultValues();
    }

    private void setupStatusComboBox() {
        statusComboBox.getItems().addAll("ACTIVE", "COMPLETED", "PAUSED", "CANCELLED");
        statusComboBox.setValue("ACTIVE");
    }

    private void setupEventHandlers() {
        simpanButton.setOnAction(event -> handleSimpan());
        batalButton.setOnAction(event -> handleBatal());

        // Update progress saat amount berubah
        targetAmountField.textProperty().addListener((obs, oldVal, newVal) -> updateProgressDisplay());
        currentAmountField.textProperty().addListener((obs, oldVal, newVal) -> updateProgressDisplay());
    }

    private void setupDefaultValues() {
        targetDatePicker.setValue(LocalDate.now().plusMonths(1));
        currentAmountField.setText("0");
        updateProgressDisplay();
    }

    private void updateProgressDisplay() {
        try {
            BigDecimal target = new BigDecimal(targetAmountField.getText().trim().isEmpty() ? "0" : targetAmountField.getText().trim());
            BigDecimal current = new BigDecimal(currentAmountField.getText().trim().isEmpty() ? "0" : currentAmountField.getText().trim());

            if (target.compareTo(BigDecimal.ZERO) > 0) {
                double progress = current.divide(target, 4, BigDecimal.ROUND_HALF_UP).doubleValue();
                progressBar.setProgress(progress);
                progressLabel.setText(String.format("%.1f%%", progress * 100));

                BigDecimal remaining = target.subtract(current);
                remainingLabel.setText(String.format("Sisa: Rp %,.2f", remaining));

                if (current.compareTo(target) >= 0) {
                    statusLabel.setText("COMPLETED");
                    statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    progressBar.setStyle("-fx-accent: green;");
                } else {
                    statusLabel.setText("IN_PROGRESS");
                    statusLabel.setStyle("-fx-text-fill: blue;");
                    progressBar.setStyle("-fx-accent: blue;");
                }
            } else {
                progressBar.setProgress(0);
                progressLabel.setText("0%");
                remainingLabel.setText("Sisa: Rp 0");
                statusLabel.setText("IN_PROGRESS");
            }

        } catch (NumberFormatException e) {
            progressBar.setProgress(0);
            progressLabel.setText("0%");
            remainingLabel.setText("Sisa: Rp 0");
        }
    }

    private void handleSimpan() {
        try {
            if (!validateInput()) {
                return;
            }

            savingGoal goal = currentGoal != null ? currentGoal : new savingGoal();

            goal.setGoalName(goalNameField.getText().trim());
            goal.setTargetAmount(new BigDecimal(targetAmountField.getText().trim()));
            goal.setCurrentAmount(new BigDecimal(currentAmountField.getText().trim()));
            goal.setTargetDate(targetDatePicker.getValue());
            goal.setStatus(statusComboBox.getValue());

            boolean success;
            if (currentGoal == null) {
                success = savingGoalDAO.addSavingGoal(goal);
            } else {
                success = savingGoalDAO.updateSavingGoal(goal);
            }

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses",
                        currentGoal == null ? "Goal berhasil ditambahkan" : "Goal berhasil diupdate");
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Gagal menyimpan goal");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        if (goalNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Nama goal tidak boleh kosong");
            goalNameField.requestFocus();
            return false;
        }

        try {
            BigDecimal targetAmount = new BigDecimal(targetAmountField.getText().trim());
            if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert(Alert.AlertType.ERROR, "Error", "Target amount harus lebih besar dari 0");
                targetAmountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Target amount harus berupa angka");
            targetAmountField.requestFocus();
            return false;
        }

        try {
            BigDecimal currentAmount = new BigDecimal(currentAmountField.getText().trim());
            if (currentAmount.compareTo(BigDecimal.ZERO) < 0) {
                showAlert(Alert.AlertType.ERROR, "Error", "Current amount tidak boleh negatif");
                currentAmountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Current amount harus berupa angka");
            currentAmountField.requestFocus();
            return false;
        }

        if (targetDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Pilih target date");
            targetDatePicker.requestFocus();
            return false;
        }

        return true;
    }

    private void handleBatal() {
        clearForm();
    }

    private void clearForm() {
        goalNameField.clear();
        targetAmountField.clear();
        currentAmountField.setText("0");
        targetDatePicker.setValue(LocalDate.now().plusMonths(1));
        statusComboBox.setValue("ACTIVE");
        currentGoal = null;
        updateProgressDisplay();
    }

    public void setGoalForEdit(savingGoal goal) {
        this.currentGoal = goal;
        goalNameField.setText(goal.getGoalName());
        targetAmountField.setText(goal.getTargetAmount().toString());
        currentAmountField.setText(goal.getCurrentAmount().toString());
        targetDatePicker.setValue(goal.getTargetDate());
        statusComboBox.setValue(goal.getStatus());
        updateProgressDisplay();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}