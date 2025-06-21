package com.example.ismoney.controller;

import com.example.ismoney.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class DashboardController {

    @FXML private Button transactionButton;
    @FXML private Button savingGoalButton;
    @FXML private Button GoalsListButton;
    @FXML private Button logOutBtn;

    @FXML
    public void initialize() {
        // Inisialisasi komponen jika diperlukan
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void handleLogoutButton() {
        Stage currentStage = (Stage) logOutBtn.getScene().getWindow();

        SceneSwitcher.logout(currentStage, "/com/example/ismoney/Login.fxml");
    }
}