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
import javafx.scene.layout.GridPane;
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

    //inisialisasi komponen
    @FXML
    public void initialize() {
        System.out.println("TransactionEditFormController initialized!");

        try {
            transactionDAO = new TransactionDAO();
            categoryDAO = new CategoryDAO();

            typeComboBox.setItems(FXCollections.observableArrayList("Pendapatan", "Pengeluaran"));

            loadCategoriesFromDatabase();

            typeComboBox.setOnAction(event -> {
                String selectedType = typeComboBox.getValue();
                if (selectedType != null) {
                    filterCategoriesByType(selectedType);
                }
            });

            categoryComboBox.setOnAction(event -> {
                Category selected = categoryComboBox.getValue();
                if (selected != null && selected.getCategoriesId() == -1) {
                    String type = typeComboBox.getValue();
                    if (type != null) {
                        showAddCategoryDialog(type);
                    } else {
                        showAlert("Pilih Tipe", "Silakan pilih tipe transaksi terlebih dahulu.");
                    }
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

        System.out.println("Transaction Details:");
        System.out.println("  ID: " + transaction.getTransactionId());
        System.out.println("  Amount: " + transaction.getAmount());
        System.out.println("  Type: " + transaction.getType());
        System.out.println("  Category ID: " + transaction.getCategoryId());
        System.out.println("  Date: " + transaction.getTransactionDate());
        System.out.println("  Note: " + transaction.getNote());

        this.currentTransaction = transaction;

        System.out.println("Current categories count: " + allCategories.size());
        if (allCategories.isEmpty()) {
            System.err.println("WARNING: No categories loaded! Reloading from database...");
            loadCategoriesFromDatabase();
        }

        System.out.println("Available categories:");
        for (Category cat : allCategories) {
            System.out.println("  - " + cat.getName() + " (ID: " + cat.getCategoriesId() + ", Type: " + cat.getType() + ")");
        }

        populateFields();
        System.out.println("DEBUG: setTransaction completed");
    }

    //Load semua kategori dari database ke cache
    private void loadCategoriesFromDatabase() {
        try {
            System.out.println("Loading categories from database...");
            allCategories = categoryDAO.getAllCategories();

            // menambah opsi "Tambah Kategori Baru" di akhir list
            allCategories.add(new Category(-1, "➕ Tambah Kategori Baru...", ""));

            categoryComboBox.setItems(FXCollections.observableArrayList(allCategories));

            System.out.println("Loaded " + (allCategories.size() - 1) + " categories from database:");
            for (Category cat : allCategories) {
                if (cat.getCategoriesId() != -1) {
                    System.out.println("  - " + cat.getName() + " (ID: " + cat.getCategoriesId() + ", Type: " + cat.getType() + ")");
                }
            }

            if (allCategories.size() <= 1) { // Hanya ada opsi "Tambah Kategori Baru"
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

            if (currentTransaction.getAmount() != null) {
                String amountText = currentTransaction.getAmount().toString();
                amountField.setText(amountText);
                System.out.println("Set amount field to: " + amountText);
                System.out.println("Amount field current value: " + amountField.getText());
            } else {
                System.err.println("WARNING: Amount is null!");
            }

            if (currentTransaction.getType() != null) {
                String typeValue = currentTransaction.getType() == TransactionType.INCOME ? "Pendapatan" : "Pengeluaran";
                typeComboBox.setValue(typeValue);
                System.out.println("Set type combo to: " + typeValue);
                System.out.println("Type combo current value: " + typeComboBox.getValue());
            } else {
                System.err.println("WARNING: Type is null!");
            }

            if (currentTransaction.getTransactionDate() != null) {
                datePicker.setValue(currentTransaction.getTransactionDate());
                System.out.println("Set date picker to: " + currentTransaction.getTransactionDate());
                System.out.println("Date picker current value: " + datePicker.getValue());
            } else {
                System.err.println("WARNING: Date is null!");
            }

            String noteText = currentTransaction.getNote() != null ? currentTransaction.getNote() : "";
            noteField.setText(noteText);
            System.out.println("Set note field to: " + (noteText.isEmpty() ? "(empty)" : noteText));
            System.out.println("Note field current value: " + noteField.getText());

            String typeValue = currentTransaction.getType() == TransactionType.INCOME ? "Pendapatan" : "Pengeluaran";
            filterCategoriesByTypeAndSetCategory(typeValue);

            System.out.println("=== DEBUG: populateFields completed ===");

        } catch (Exception e) {
            System.err.println("Error in populateFields: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal mengisi form dengan data transaksi: " + e.getMessage());
        }
    }

    //Filter kategori + set kategori sesuai transaksi (Ketika form pertama kali di load)
    private void filterCategoriesByTypeAndSetCategory(String selectedType) {
        try {
            System.out.println("Filtering categories for type: " + selectedType);

            List<Category> filtered = allCategories.stream()
                    .filter(c -> c.getCategoriesId() == -1 || c.getType().equals(selectedType))
                    .toList();

            categoryComboBox.setItems(FXCollections.observableArrayList(filtered));
            System.out.println("Filtered categories count: " + (filtered.size() - 1)); // -1 untuk opsi tambah kategori

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
                    System.out.println("Available categories after filtering:");
                    filtered.forEach(cat -> {
                        if (cat.getCategoriesId() != -1) {
                            System.out.println("  - " + cat.getName() + " (ID: " + cat.getCategoriesId() + ", Type: " + cat.getType() + ")");
                        }
                    });
                }
            }

        } catch (Exception e) {
            System.err.println("Error filtering categories and setting category: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //memfilter kategori berdasarkan tipe transaksi yang dipilih (Ketika user mengubah tipe transaksi)
    private void filterCategoriesByType(String selectedType) {
        try {
            System.out.println("User changed type to: " + selectedType);

            List<Category> filtered = allCategories.stream()
                    .filter(c -> c.getCategoriesId() == -1 || c.getType().equals(selectedType))
                    .toList();

            categoryComboBox.setItems(FXCollections.observableArrayList(filtered));
            categoryComboBox.setValue(null);

            System.out.println("Filtered categories for user selection: " + (filtered.size() - 1) + " categories");

        } catch (Exception e) {
            System.err.println("Error filtering categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Dialog menampilkan dialog tambah kategori
    private void showAddCategoryDialog(String type) {
        Dialog<Category> dialog = new Dialog<>();
        dialog.setTitle("Tambah Kategori Baru");

        Label nameLabel = new Label("Nama:");
        TextField nameField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showAlert("Validasi Gagal", "Nama kategori tidak boleh kosong!");
                    return null;
                }

                Category newCategory = new Category(0, name, type);
                try {
                    if (categoryDAO.addCategory(newCategory)) {
                        loadCategoriesFromDatabase();

                        Category addedCategory = allCategories.stream()
                                .filter(c -> c.getName().equals(name) && c.getType().equals(type))
                                .findFirst()
                                .orElse(newCategory);

                        System.out.println("Successfully added category: " + addedCategory.getName() + " with ID: " + addedCategory.getCategoriesId());
                        return addedCategory;
                    } else {
                        showAlert("Kesalahan", "Gagal menyimpan kategori ke database");
                        return null;
                    }
                } catch (Exception e) {
                    System.err.println("Error saving category: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("Kesalahan", "Error menyimpan kategori: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newCategory -> {
            filterCategoriesByType(type);
            categoryComboBox.setValue(newCategory);
            System.out.println("Selected new category: " + newCategory.getName());
        });
    }

    //handlers save utk menyimpan transaksi yg diedit
    @FXML
    private void handleSaveTransaction() {
        System.out.println("Update transaction button clicked!");

        if (currentTransaction == null) {
            showAlert("Error", "Tidak ada transaksi yang dipilih untuk diedit.");
            return;
        }

        if (!validateInput()) return;

        try {
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

            boolean success = transactionDAO.updateTransaction(currentTransaction);

            if (success) {
                showSuccessAlert("Berhasil", "Transaksi berhasil diperbarui!");

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

        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().getCategoriesId() == -1) {
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