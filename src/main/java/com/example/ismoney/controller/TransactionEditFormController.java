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

    // Menginisialisasi semua komponen dan event handlers
    @FXML
    public void initialize() {
        System.out.println("TransactionEditFormController initialized!");

        try {
            //inisialisasi DAO Objects
            transactionDAO = new TransactionDAO();
            categoryDAO = new CategoryDAO();
            //Drpdown untuk tipe transaksi
            typeComboBox.setItems(FXCollections.observableArrayList("Pendapatan", "Pengeluaran"));
            //load categories dari db
            loadCategories();
            //memfilter kategori berdasarkan tipe transaksi
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
        this.currentTransaction = transaction;
        populateFields();
    }

    //Mengisi form fields dengan data dari transaksi yang sedang diedit
    private void populateFields() {
        if (currentTransaction != null) {
            //set data yg diedit
            amountField.setText(currentTransaction.getAmount().toString());

            String typeValue = currentTransaction.getType() == TransactionType.INCOME ? "Pendapatan" : "Pengeluaran";
            typeComboBox.setValue(typeValue);

            filterCategoriesByType(typeValue);

            datePicker.setValue(currentTransaction.getTransactionDate());
            noteField.setText(currentTransaction.getNote() != null ? currentTransaction.getNote() : "");

            try {
                Category category = categoryDAO.getCategoryById(currentTransaction.getCategoryId());
                if (category != null) {
                    for (Category c : categoryComboBox.getItems()) {
                        if (c.getCategoriesId().equals(category.getCategoriesId())) {
                            categoryComboBox.setValue(c);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading category for edit: " + e.getMessage());
            }
        }
    }

    private List<Category> allCategories = new ArrayList<>();

    //Memuat semua kategori dari database ke cache lokal
    private void loadCategories() {
        try {
            allCategories = categoryDAO.getAllCategories();
            categoryComboBox.setItems(FXCollections.observableArrayList(allCategories));
            System.out.println("Loaded " + allCategories.size() + " categories for edit form");
        } catch (Exception e) {
            System.err.println("Error loading categories: " + e.getMessage());
        }
    }

    //Filter kategori berdasarkan tipe transaksi yang dipilih
    private void filterCategoriesByType(String selectedType) {
        try {
            List<Category> filtered = allCategories.stream()
                    .filter(c -> c.getType().equals(selectedType))
                    .toList();
            categoryComboBox.setItems(FXCollections.observableArrayList(filtered));

            System.out.println("Filtered categories for type '" + selectedType + "': " + filtered.size() + " categories");
        } catch (Exception e) {
            System.err.println("Error filtering categories: " + e.getMessage());
        }
    }

    //Handler untuk tombol Save utk menyimpan perubahan transaksi
    @FXML
    private void handleSaveTransaction() {
        System.out.println("Update transaction button clicked!");
        // Validasi bahwa ada transaksi yang sedang diedit
        if (currentTransaction == null) {
            showAlert("Error", "Tidak ada transaksi yang dipilih untuk diedit.");
            return;
        }
        // Validasi input form
        if (!validateInput()) return;

        try {
            // Ambil data dari form fields
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            String type = typeComboBox.getValue();
            Category category = categoryComboBox.getValue();
            LocalDate date = datePicker.getValue();
            String note = noteField.getText().trim();

            // Update data transaksi dengan nilai baru
            currentTransaction.setAmount(amount);
            currentTransaction.setType(type.equals("Pendapatan") ? TransactionType.INCOME : TransactionType.OUTCOME);
            currentTransaction.setCategoryId(category.getCategoriesId());
            currentTransaction.setNote(note.isEmpty() ? null : note);
            currentTransaction.setTransactionDate(date);

            System.out.println("Attempting to update transaction: " + currentTransaction);

            // Simpan perubahan ke database
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

    //Validasi input form sebelum menyimpan data
    private boolean validateInput() {
        //nominal
        if (amountField.getText() == null || amountField.getText().trim().isEmpty()) {
            showAlert("Validasi Gagal", "Nominal harus diisi!");
            amountField.requestFocus();
            return false;
        }
        //format dan nominal
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
        //tipe transaksi
        if (typeComboBox.getValue() == null) {
            showAlert("Validasi Gagal", "Tipe transaksi harus dipilih!");
            typeComboBox.requestFocus();
            return false;
        }
        //kategori
        if (categoryComboBox.getValue() == null) {
            showAlert("Validasi Gagal", "Kategori harus dipilih!");
            categoryComboBox.requestFocus();
            return false;
        }
        //tanggal
        if (datePicker.getValue() == null) {
            showAlert("Validasi Gagal", "Tanggal transaksi harus dipilih!");
            datePicker.requestFocus();
            return false;
        }

        return true;
    }

    //Handlar tombol batal utk menutup form tanpa menyimpan perubahan
    @FXML
    private void handleCancel() {
        closeForm();
    }

    //Menutup form edit transaksi
    private void closeForm() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }

    //Menampilkan alert dialog untuk pesan peringatan atau error
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //Menampilkan alert dialog untuk pesan sukses
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}