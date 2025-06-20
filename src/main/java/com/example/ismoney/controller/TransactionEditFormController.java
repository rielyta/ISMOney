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

public class TransactionEditFormController {

    @FXML private TextField amountField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextArea noteField;

    private TransactionDAO transactionDAO;
    private CategoryDAO categoryDAO;
    private Transaction currentTransaction;

    @FXML
    public void initialize() {
        System.out.println("TransactionEditFormController initialized!");

        try {
            transactionDAO = new TransactionDAO();
            categoryDAO = new CategoryDAO();

            typeComboBox.setItems(FXCollections.observableArrayList("Pendapatan", "Pengeluaran"));

            // Load categories
            loadCategories();

            // Setup event handlers
            typeComboBox.setOnAction(event -> {
                String selectedType = typeComboBox.getValue();
                if (selectedType != null) {
                    filterCategoriesByType(selectedType);
                }
            });

        } catch (Exception e) {
            System.err.println("Error initializing TransactionEditFormController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setTransaction(Transaction transaction) {
        this.currentTransaction = transaction;
        populateFields();
    }

    private void populateFields() {
        if (currentTransaction != null) {
            amountField.setText(currentTransaction.getAmount().toString());
            typeComboBox.setValue(currentTransaction.getType() == TransactionType.INCOME ? "Pendapatan" : "Pengeluaran");
            datePicker.setValue(currentTransaction.getTransactionDate());
            noteField.setText(currentTransaction.getNote() != null ? currentTransaction.getNote() : "");

            // Set category
            try {
                Category category = categoryDAO.getCategoryById(currentTransaction.getCategoryId());
                if (category != null) {
                    categoryComboBox.setValue(category);
                }
            } catch (Exception e) {
                System.err.println("Error loading category for edit: " + e.getMessage());
            }
        }
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryDAO.getAllCategories();
            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        } catch (Exception e) {
            System.err.println("Error loading categories: " + e.getMessage());
        }
    }

    private void filterCategoriesByType(String selectedType) {
        try {
            List<Category> allCategories = categoryDAO.getAllCategories();
            List<Category> filtered = allCategories.stream()
                    .filter(c -> c.getType().equals(selectedType))
                    .toList();
            categoryComboBox.setItems(FXCollections.observableArrayList(filtered));
            categoryComboBox.setValue(null);
        } catch (Exception e) {
            System.err.println("Error filtering categories: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveTransaction() {
        System.out.println("Update transaction button clicked!");

        if (currentTransaction == null) {
            showAlert("Error", "Tidak ada transaksi yang dipilih untuk diedit.");
            return;
        }

        // Validate input
        if (!validateInput()) return;

        try {
            // Update transaction data
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            String type = typeComboBox.getValue();
            Category category = categoryComboBox.getValue();
            LocalDate date = datePicker.getValue();
            String note = noteField.getText().trim();

            currentTransaction.setAmount(amount);
            currentTransaction.setType(type.equals("Pendapatan") ? TransactionType.INCOME : TransactionType.OUTCOME);
            currentTransaction.setCategoryId(category.getCategoriesId());
            currentTransaction.setNote(note.isEmpty() ? null : note);
            currentTransaction.setTransactionDate(date);

            System.out.println("Attempting to update transaction: " + currentTransaction);

            // Save to database
            boolean success = transactionDAO.updateTransaction(currentTransaction);

            if (success) {
                showSuccessAlert("Berhasil", "Transaksi berhasil diperbarui!");
                closeForm();
            } else {
                showAlert("Error", "Gagal memperbarui transaksi ke database.");
            }

        } catch (Exception e) {
            System.err.println("Error updating transaction: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Terjadi kesalahan saat memperbarui transaksi: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        // Validate amount
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

        // Validate type
        if (typeComboBox.getValue() == null) {
            showAlert("Validasi Gagal", "Tipe transaksi harus dipilih!");
            typeComboBox.requestFocus();
            return false;
        }

        // Validate category
        if (categoryComboBox.getValue() == null) {
            showAlert("Validasi Gagal", "Kategori harus dipilih!");
            categoryComboBox.requestFocus();
            return false;
        }

        // Validate date
        if (datePicker.getValue() == null) {
            showAlert("Validasi Gagal", "Tanggal transaksi harus dipilih!");
            datePicker.requestFocus();
            return false;
        }

        return true;
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