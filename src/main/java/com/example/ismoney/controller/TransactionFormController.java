package com.example.ismoney.controller;


import com.example.ismoney.model.Category;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class TransactionFormController {

    @FXML private TextField amountField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextArea noteField;

    @FXML
    public void initialize() {
        // Isi tipe transaksi
        typeComboBox.setItems(FXCollections.observableArrayList("INCOME", "OUTCOME"));

        // Isi kategori (dummy data sementara)
        categoryComboBox.setItems(FXCollections.observableArrayList(
                new Category("1", "Gaji", "INCOME"),
                new Category("2", "Makan", "OUTCOME"),
                new Category("3", "Transport", "OUTCOME")
        ));

        datePicker.setValue(LocalDate.now());
    }

    @FXML
    private void handleSaveTransaction() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            String type = typeComboBox.getValue();
            Category category = categoryComboBox.getValue();
            LocalDate date = datePicker.getValue();
            String note = noteField.getText();

            if (type == null || category == null || date == null) {
                showAlert("Validasi Gagal", "Semua field wajib diisi!");
                return;
            }

            Transaction newTransaction = new Transaction(
                    null, // ID auto-generate
                    "USER123", // userId contoh (nanti ambil dari session)
                    amount,
                    type.equals("INCOME") ? TransactionType.INCOME : TransactionType.OUTCOME,
                    category,
                    note,
                    date
            );

            // TODO: Kirim ke service/DAO untuk disimpan
            System.out.println("Transaksi disimpan: " + newTransaction);

            closeForm();

        } catch (NumberFormatException e) {
            showAlert("Input Error", "Nominal harus berupa angka.");
        }
    }

    @FXML
    private void handleCancel() {
        closeForm();
    }

    private void closeForm() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

