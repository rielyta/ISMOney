package com.example.ismoney.controller;

import com.example.ismoney.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class DashboardController {

    @FXML private Button transactionButton;
    @FXML private Button savingGoalButton;
    @FXML private Button logOutBtn;

    @FXML
    public void initialize() {
    }

    @FXML
    private void handleTransactionButton() {
        try {
            // Load the FXML file for transaction form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ismoney/Transaction/TransactionForm.fxml"));
            Parent root = loader.load();

            Stage transactionStage = new Stage();
            transactionStage.setTitle("Tambah Transaksi");
            transactionStage.setScene(new Scene(root));

            transactionStage.initModality(Modality.APPLICATION_MODAL);

            // Show the transaction form and wait for it to be closed
            transactionStage.showAndWait();

        } catch (IOException e) {
            showAlert("Error", "Gagal membuka form transaksi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSavingGoalButton() {
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ismoney/SavingGoal/SavingGoal.fxml"));
            Parent root = loader.load();

            Stage GoalsStage = new Stage();
            GoalsStage.setTitle("Saving Goals Anda");
            GoalsStage.setScene(new Scene(root));
            GoalsStage.initModality(Modality.APPLICATION_MODAL);

            GoalsStage.showAndWait();

        } catch (IOException e) {
            showAlert("Error", "Gagal membuka form saving goals: " + e.getMessage());
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