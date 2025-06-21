package com.example.ismoney.controller;

import com.example.ismoney.dao.CategoryDAO;
import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.model.Category;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import com.example.ismoney.util.SceneSwitcher;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

    // Cache untuk menyimpan semua kategori
    private List<Category> allCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        System.out.println("TransactionEditFormController initialized!");

        try {
            transactionDAO = new TransactionDAO();
            categoryDAO = new CategoryDAO();

            typeComboBox.setItems(FXCollections.observableArrayList("Pendapatan", "Pengeluaran"));

            // Load semua kategori dari database ke cache
            loadCategoriesFromDatabase();

            // Setup event handlers utk mendapatkan kategori sesuai tipe
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

    //Set transaksi yang akan diedit dan populate form fields
    public void setTransaction(Transaction transaction) {
        System.out.println("DEBUG: setTransaction called");

        if (transaction == null) {
            System.err.println("ERROR: Transaction is null!");
            showAlert("Error", "Data transaksi tidak valid!");
            return;
        }

        // tampilkan detail transaksi (utk debug saja)
        System.out.println("Transaction Details:");
        System.out.println("  ID: " + transaction.getTransactionId());
        System.out.println("  Amount: " + transaction.getAmount());
        System.out.println("  Type: " + transaction.getType());
        System.out.println("  Category ID: " + transaction.getCategoryId());
        System.out.println("  Date: " + transaction.getTransactionDate());
        System.out.println("  Note: " + transaction.getNote());

        this.currentTransaction = transaction;

        // Pastikan categories sudah ter-load
        System.out.println("Current categories count: " + allCategories.size());
        if (allCategories.isEmpty()) {
            System.err.println("WARNING: No categories loaded! Reloading from database...");
            loadCategoriesFromDatabase();
        }

        // Debug: tampilkan categories yang ter-load
        System.out.println("Available categories:");
        for (Category cat : allCategories) {
            System.out.println("  - " + cat.getName() + " (ID: " + cat.getCategoriesId() + ", Type: " + cat.getType() + ")");
        }

        // Populate fields setelah data siap
        populateFields();
        System.out.println("=== DEBUG: setTransaction completed ===");
    }


    //Load semua kategori dari database ke cache
    private void loadCategoriesFromDatabase() {
        try {
            System.out.println("Loading categories from database...");
            allCategories = categoryDAO.getAllCategories();
            categoryComboBox.setItems(FXCollections.observableArrayList(allCategories));

            System.out.println("Loaded " + allCategories.size() + " categories from database:");
            for (Category cat : allCategories) {
                System.out.println("  - " + cat.getName() + " (ID: " + cat.getCategoriesId() + ", Type: " + cat.getType() + ")");
            }

            if (allCategories.isEmpty()) {
                System.err.println("WARNING: No categories found in database!");
                showAlert("Peringatan", "Tidak ada kategori yang tersedia. Silakan tambah kategori terlebih dahulu.");
            }

        } catch (Exception e) {
            System.err.println("Error loading categories from database: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal memuat kategori dari database: " + e.getMessage());
        }
    }


    //Mengisi form fields dengan data dari transaksi yang sedang diedit
    private void populateFields() {
        System.out.println("DEBUG: populateFields called");

        if (currentTransaction == null) {
            System.err.println("ERROR: currentTransaction is null in populateFields!");
            return;
        }

        try {
            System.out.println("Populating fields for transaction ID: " + currentTransaction.getTransactionId());

            // Set nominal transaksi
            if (currentTransaction.getAmount() != null) {
                String amountText = currentTransaction.getAmount().toString();
                amountField.setText(amountText);
                System.out.println("Set amount field to: " + amountText);
                System.out.println("Amount field current value: " + amountField.getText());
            } else {
                System.err.println("WARNING: Amount is null!");
            }

            // Set tipe transaksi
            if (currentTransaction.getType() != null) {
                String typeValue = currentTransaction.getType() == TransactionType.INCOME ? "Pendapatan" : "Pengeluaran";
                typeComboBox.setValue(typeValue);
                System.out.println("Set type combo to: " + typeValue);
                System.out.println("Type combo current value: " + typeComboBox.getValue());
            } else {
                System.err.println("WARNING: Type is null!");
            }

            // Set tanggal transaksi
            if (currentTransaction.getTransactionDate() != null) {
                datePicker.setValue(currentTransaction.getTransactionDate());
                System.out.println("Set date picker to: " + currentTransaction.getTransactionDate());
                System.out.println("Date picker current value: " + datePicker.getValue());
            } else {
                System.err.println("WARNING: Date is null!");
            }

            // Set catatan
            String noteText = currentTransaction.getNote() != null ? currentTransaction.getNote() : "";
            noteField.setText(noteText);
            System.out.println("Set note field to: " + (noteText.isEmpty() ? "(empty)" : noteText));
            System.out.println("Note field current value: " + noteField.getText());

            // Filter kategori dan set kategori yang sesuai
            String typeValue = currentTransaction.getType() == TransactionType.INCOME ? "Pendapatan" : "Pengeluaran";
            filterCategoriesByTypeAndSetCategory(typeValue);

            System.out.println("=== DEBUG: populateFields completed ===");

        } catch (Exception e) {
            System.err.println("Error in populateFields: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal mengisi form dengan data transaksi: " + e.getMessage());
        }
    }


    private void filterCategoriesByTypeAndSetCategory(String selectedType) {
        try {
            System.out.println("Filtering categories for type: " + selectedType);

            // Filter kategori berdasarkan tipe
            List<Category> filtered = allCategories.stream()
                    .filter(c -> c.getType().equals(selectedType))
                    .toList();

            // Update dropdown dengan kategori yang sudah difilter
            categoryComboBox.setItems(FXCollections.observableArrayList(filtered));
            System.out.println("Filtered categories count: " + filtered.size());

            // Langsung set kategori yang sesuai dengan transaksi saat ini
            if (currentTransaction != null) {
                Category currentCategory = filtered.stream()
                        .filter(c -> c.getCategoriesId().equals(currentTransaction.getCategoryId()))
                        .findFirst()
                        .orElse(null);

                if (currentCategory != null) {
                    categoryComboBox.setValue(currentCategory);
                    System.out.println("Successfully set category: " + currentCategory.getName() + " (ID: " + currentCategory.getCategoriesId() + ")");
                } else {
                    System.err.println("Category with ID " + currentTransaction.getCategoryId() + " not found in filtered list");
                    // Debug: tampilkan semua kategori yang tersedia
                    System.out.println("Available categories after filtering:");
                    filtered.forEach(cat -> System.out.println("  - " + cat.getName() + " (ID: " + cat.getCategoriesId() + ", Type: " + cat.getType() + ")"));
                }
            }

        } catch (Exception e) {
            System.err.println("Error filtering categories and setting category: " + e.getMessage());
            e.printStackTrace();
        }
    }



    //Filter kategori berdasarkan tipe transaksi yang dipilih
    private void filterCategoriesByType(String selectedType) {
        try {
            System.out.println("User changed type to: " + selectedType);

            // Filter kategori berdasarkan tipe
            List<Category> filtered = allCategories.stream()
                    .filter(c -> c.getType().equals(selectedType))
                    .toList();

            // Update dropdown dengan kategori yang sudah difilter
            categoryComboBox.setItems(FXCollections.observableArrayList(filtered));

            // Reset pilihan kategori karena user mengubah tipe
            categoryComboBox.setValue(null);

            System.out.println("Filtered categories for user selection: " + filtered.size() + " categories");

        } catch (Exception e) {
            System.err.println("Error filtering categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //handlers save utk menyimpan transaksi yg diedit
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

                // Navigate kembali ke TransactionList
                navigateToTransactionList();
            } else {
                showAlert("Error", "Gagal memperbarui transaksi ke database.");
            }

        } catch (Exception e) {
            System.err.println("Error updating transaction: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Terjadi kesalahan saat memperbarui transaksi: " + e.getMessage());
        }
    }

    //validasi input
    private boolean validateInput() {
        // Validate nominal
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

    //handlers tombol cancel agar kembali ke transaction list
    @FXML
    private void handleCancel() {
        navigateToTransactionList();
    }


    // Method untuk navigation ke TransactionList
    private void navigateToTransactionList() {
        try {
            Stage currentStage = (Stage) amountField.getScene().getWindow();
            SceneSwitcher.switchTo("Transaction/TransactionList.fxml", currentStage);
            System.out.println("Successfully navigated back to TransactionList");
        } catch (Exception e) {
            System.err.println("Error navigating to TransactionList: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal kembali ke halaman transaksi.");
        }
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