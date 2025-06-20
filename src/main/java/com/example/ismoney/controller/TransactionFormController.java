package com.example.ismoney.controller;

import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.model.Category;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionFormController {

    @FXML private TextField amountField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextArea noteField;

    private TransactionDAO transactionDAO;
    private static final Integer CURRENT_USER_ID = 1;

    private List<Category> defaultCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        transactionDAO = new TransactionDAO();

        typeComboBox.setItems(FXCollections.observableArrayList("INCOME", "OUTCOME"));
        datePicker.setValue(LocalDate.now());

        setupCategoryComboBox();

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
    }

    private void setupCategoryComboBox() {
        defaultCategories.clear();
        defaultCategories.add(new Category(1, "Transportasi", "OUTCOME"));
        defaultCategories.add(new Category(2, "Belanja Bulanan", "OUTCOME"));
        defaultCategories.add(new Category(3, "Hiburan", "OUTCOME"));
        defaultCategories.add(new Category(4, "Gaji", "INCOME"));
        defaultCategories.add(new Category(5, "Bonus", "INCOME"));
        defaultCategories.add(new Category(-1, "➕ Tambah Kategori Baru...", ""));

        categoryComboBox.setItems(FXCollections.observableArrayList(defaultCategories));
    }

    private void filterCategoriesByType(String selectedType) {
        List<Category> filtered = defaultCategories.stream()
                .filter(c -> c.getCategoriesId() == -1 || c.getType().equals(selectedType))
                .toList();
        categoryComboBox.setItems(FXCollections.observableArrayList(filtered));
        categoryComboBox.setValue(null);
    }

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

                Category category = new Category(defaultCategories.size() + 100, name, type);
                defaultCategories.add(defaultCategories.size() - 1, category); // Simpan sebelum "➕"
                return category;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newCategory -> {
            filterCategoriesByType(type);
            categoryComboBox.setValue(newCategory);
        });
    }

    @FXML
    private void handleSaveTransaction() {
        try {
            if (!validateInput()) return;

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
