package com.example.ismoney.controller;

import com.example.ismoney.dao.CategoryDAO;
import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.model.Category;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransactionFormController {

    @FXML private TextField amountField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextArea noteField;

    private TransactionDAO transactionDAO;
    private CategoryDAO categoryDAO;
    private static final Integer CURRENT_USER_ID = 1; // Nanti ambil dari session

    @FXML
    public void initialize() {
        transactionDAO = new TransactionDAO();
        categoryDAO = new CategoryDAO();

        typeComboBox.setItems(FXCollections.observableArrayList("INCOME", "OUTCOME"));

        datePicker.setValue(LocalDate.now());

        loadAllCategories();

        typeComboBox.setOnAction(event -> {
            String selectedType = typeComboBox.getValue();
            if (selectedType != null) {
                loadCategoriesByType(selectedType);
            }
        });
    }

    private void loadAllCategories() {
        try {
            List<Category> categories = categoryDAO.getAllCategories();
            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        } catch (Exception e) {
            showAlert("Database Error", "Gagal memuat kategori: " + e.getMessage());
        }
    }

    private void loadCategoriesByType(String type) {
        try {
            List<Category> categories = categoryDAO.getCategoriesByType(type);
            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
            categoryComboBox.setValue(null); // Reset selection
        } catch (Exception e) {
            showAlert("Database Error", "Gagal memuat kategori: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveTransaction() {
        try {
            // Validate input
            if (!validateInput()) {
                return;
            }

            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            String type = typeComboBox.getValue();
            Category category = categoryComboBox.getValue();
            LocalDate date = datePicker.getValue();
            String note = noteField.getText().trim();

            Transaction newTransaction = new Transaction();
            newTransaction.setUserId(CURRENT_USER_ID);
            newTransaction.setAmount(amount);
            newTransaction.setType(type.equals("Pendapatan") ? TransactionType.INCOME : TransactionType.OUTCOME);
            newTransaction.setCategoryId(category.getCategoriesId());
            newTransaction.setNote(note.isEmpty() ? null : note);
            newTransaction.setTransactionDate(date);

            boolean success = transactionDAO.saveTransaction(newTransaction);

            if (success) {
                showSuccessAlert("Berhasil", "Transaksi berhasil disimpan!");
                clearForm();
                closeForm();
            } else {
                showAlert("Error", "Gagal menyimpan transaksi ke database.");
            }

        } catch (NumberFormatException e) {
            showAlert("Input Error", "Nominal harus berupa angka yang valid.");
        } catch (Exception e) {
            showAlert("Error", "Terjadi kesalahan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInput() {
        if (amountField.getText() == null || amountField.getText().trim().isEmpty()) {
            showAlert("Validasi Gagal", "Nominal harus diisi!");
            amountField.requestFocus();
            return false;
        }

        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Validasi Gagal", "Nominal harus lebih dari 0!");
                amountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validasi Gagal", "Nominal harus berupa angka yang valid!");
            amountField.requestFocus();
            return false;
        }

        if (typeComboBox.getValue() == null) {
            showAlert("Validasi Gagal", "Tipe transaksi harus dipilih!");
            typeComboBox.requestFocus();
            return false;
        }

        if (categoryComboBox.getValue() == null) {
            showAlert("Validasi Gagal", "Kategori harus dipilih!");
            categoryComboBox.requestFocus();
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert("Validasi Gagal", "Tanggal transaksi harus dipilih!");
            datePicker.requestFocus();
            return false;
        }

        return true;
    }

    private void clearForm() {
        amountField.clear();
        typeComboBox.setValue(null);
        categoryComboBox.setValue(null);
        datePicker.setValue(LocalDate.now());
        noteField.clear();
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
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}