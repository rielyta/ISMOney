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
import java.net.URL;

public class DashboardController {

    @FXML private Button transactionButton;
    @FXML private Button savingGoalButton;
    @FXML private Button GoalsListButton;

    @FXML
    public void initialize() {
        // Inisialisasi komponen jika diperlukan
    }

    @FXML
    private void handleTransactionButton() {
        try {
            String[] possiblePaths = {
                    "/com/example/ismoney/Transaction/TransactionList.fxml",
                    "/Transaction/TransactionList.fxml",
                    "/TransactionList.fxml",
                    "Transaction/TransactionList.fxml",
                    "TransactionList.fxml"
            };

            URL fxmlUrl = null;
            String workingPath = null;

            for (String path : possiblePaths) {
                fxmlUrl = getClass().getResource(path);
                if (fxmlUrl != null) {
                    workingPath = path;
                    System.out.println("Found FXML at: " + path);
                    break;
                } else {
                    System.out.println("FXML not found at: " + path);
                }
            }

            if (fxmlUrl == null) {
                showAlert("Kesalahan", "File FXML tidak ditemukan di semua lokasi yang dicoba.\n\nPastikan file TransactionList.fxml atau TransactionForm.fxml ada di folder resources.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Stage transactionStage = new Stage();
            transactionStage.setTitle(workingPath.contains("List") ? "Daftar Transaksi" : "Tambah Transaksi");
            transactionStage.setScene(new Scene(root));

            transactionStage.initModality(Modality.APPLICATION_MODAL);

            transactionStage.showAndWait();

        } catch (IOException e) {
            showAlert("Kesalahan", "Gagal membuka form: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Kesalahan", "Terjadi kesalahan tidak terduga: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoalsListButton() {
        try {
            String[] possiblePaths = {
                    "/com/example/ismoney/savingGoals/savingGoalList.fxml",
                    "/savingGoals/savingGoalList.fxml",
                    "/savingGoalList.fxml",
                    "savingGoals/savingGoalList.fxml",
                    "savingGoalList.fxml"
            };

            URL fxmlUrl = null;
            String workingPath = null;

            for (String path : possiblePaths) {
                fxmlUrl = getClass().getResource(path);
                if (fxmlUrl != null) {
                    workingPath = path;
                    System.out.println("Found SavingGoalList FXML at: " + path);
                    break;
                } else {
                    System.out.println("SavingGoal FXML not found at: " + path);
                }
            }

            if (fxmlUrl == null) {
                showAlert("Info", "File SavingGoal FXML tidak ditemukan.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Stage goalsStage = new Stage();
            goalsStage.setTitle("Target Tabungan Anda");
            goalsStage.setScene(new Scene(root));
            goalsStage.initModality(Modality.APPLICATION_MODAL);

            goalsStage.showAndWait();

        } catch (IOException e) {
            showAlert("Kesalahan", "Gagal membuka list target tabungan: " + e.getMessage());
            e.printStackTrace();
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
}