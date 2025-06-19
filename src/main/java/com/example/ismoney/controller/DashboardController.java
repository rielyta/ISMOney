package com.example.ismoney.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML private Button transactionButton;
    @FXML private Button savingGoalButton;

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
        // TODO: Implement saving goal functionality
        showAlert("Info", "Fitur Saving Goal akan segera hadir!");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}