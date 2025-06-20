package com.example.ismoney.controller;

import com.example.ismoney.dao.CategoryDAO;
import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.dao.UserDAOImpl;
import com.example.ismoney.model.Category;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import com.example.ismoney.model.user.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private CategoryDAO categoryDAO;
    private UserDAOImpl userDAO;
    private Integer currentUserId;

    private List<Category> defaultCategories = new ArrayList<>();

    @FXML
    public void initialize() {
        System.out.println("TransactionFormController initialized!");

        try {
            transactionDAO = new TransactionDAO();
            categoryDAO = new CategoryDAO();
            userDAO = new UserDAOImpl();

            // Use the same logic as TransactionListController to get current user
            currentUserId = getCurrentLoggedInUserId();
            System.out.println("Using user ID for transaction form: " + currentUserId);

            // Setup basic controls
            typeComboBox.setItems(FXCollections.observableArrayList("Pendapatan", "Pengeluaran"));
            datePicker.setValue(LocalDate.now());

            // Load categories from database first, then add defaults if needed
            loadCategoriesFromDatabase();
            setupCategoryComboBox();

            // Setup event handlers
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

            System.out.println("TransactionFormController setup completed!");

        } catch (Exception e) {
            System.err.println("Error initializing TransactionFormController: " + e.getMessage());
            e.printStackTrace();
            showAlert("Kesalahan", "Gagal menginisialisasi form: " + e.getMessage());
        }
    }

    private Integer getCurrentLoggedInUserId() {
        try {
            // First try to get the most recently created user (likely the logged-in one)
            Integer latestUserId = getLatestUserId();
            if (latestUserId != null) {
                System.out.println("Using latest user ID: " + latestUserId);
                return latestUserId;
            }

            // Fallback to first existing user
            Integer existingUserId = getFirstExistingUserId();
            if (existingUserId != null) {
                System.out.println("Using first existing user ID: " + existingUserId);
                return existingUserId;
            }

            // If no users exist, create a test user
            User testUser = new User("testuser", "test@example.com", userDAO.hashPassword("password123"));
            if (userDAO.save(testUser)) {
                System.out.println("Created test user with ID: " + testUser.getId());
                return testUser.getId();
            }

            System.out.println("No users found, using default ID: 1");
            return 1;
        } catch (Exception e) {
            System.err.println("Error getting current user ID: " + e.getMessage());
            return 1;
        }
    }

    private Integer getLatestUserId() {
        try (Connection conn = com.example.ismoney.database.DatabaseConfig.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users ORDER BY created_at DESC, id DESC LIMIT 1");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            System.err.println("Error getting latest user ID: " + e.getMessage());
        }
        return null;
    }

    private Integer getFirstExistingUserId() {
        try (Connection conn = com.example.ismoney.database.DatabaseConfig.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users ORDER BY id LIMIT 1");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            System.err.println("Error getting existing user ID: " + e.getMessage());
        }
        return null;
    }

    private void loadCategoriesFromDatabase() {
        try {
            List<Category> dbCategories = categoryDAO.getAllCategories();
            System.out.println("Loaded " + dbCategories.size() + " categories from database");

            defaultCategories.clear();
            defaultCategories.addAll(dbCategories);

            // Add default categories if database is empty
            if (dbCategories.isEmpty()) {
                System.out.println("No categories in database, adding defaults...");
                addDefaultCategories();
            }

        } catch (Exception e) {
            System.err.println("Error loading categories from database: " + e.getMessage());
            e.printStackTrace();
            // Fallback to hardcoded categories
            addDefaultCategories();
        }
    }

    private void addDefaultCategories() {
        defaultCategories.clear();
        defaultCategories.add(new Category(1, "Transportasi", "Pengeluaran"));
        defaultCategories.add(new Category(2, "Belanja Bulanan", "Pengeluaran"));
        defaultCategories.add(new Category(3, "Hiburan", "Pengeluaran"));
        defaultCategories.add(new Category(4, "Gaji", "Pendapatan"));
        defaultCategories.add(new Category(5, "Bonus", "Pendapatan"));
    }

    private void setupCategoryComboBox() {
        // Add the "Add new category" option
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

                // Try to save to database first
                Category newCategory = new Category(0, name, type);
                try {
                    if (categoryDAO.addCategory(newCategory)) {
                        // Add to local list (remove the "add new" option first)
                        defaultCategories.remove(defaultCategories.size() - 1);
                        defaultCategories.add(newCategory);
                        defaultCategories.add(new Category(-1, "➕ Tambah Kategori Baru...", ""));
                        return newCategory;
                    } else {
                        showAlert("Kesalahan", "Gagal menyimpan kategori ke database");
                        return null;
                    }
                } catch (Exception e) {
                    System.err.println("Error saving category: " + e.getMessage());
                    // Fallback: add to local list only
                    Category localCategory = new Category(defaultCategories.size() + 100, name, type);
                    defaultCategories.add(defaultCategories.size() - 1, localCategory);
                    return localCategory;
                }
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
        System.out.println("Save transaction button clicked!");

        // Check if we have a valid user ID
        if (currentUserId == null) {
            showAlert("Error", "Tidak ada user yang valid. Silakan login terlebih dahulu.");
            return;
        }

        // 1. Validasi input dari form
        if (!validateInput()) return;

        try {
            // 2. Ambil data dari form
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            String type = typeComboBox.getValue();
            Category category = categoryComboBox.getValue();
            LocalDate date = datePicker.getValue();
            String note = noteField.getText().trim();

            // 3. Buat object Transaction baru
            Transaction newTransaction = new Transaction();
            newTransaction.setUserId(currentUserId); // Use consistent user ID
            newTransaction.setAmount(amount);
            newTransaction.setType(type.equals("Pendapatan") ? TransactionType.INCOME : TransactionType.OUTCOME);
            newTransaction.setCategoryId(category.getCategoriesId());
            newTransaction.setNote(note.isEmpty() ? null : note);
            newTransaction.setTransactionDate(date);

            System.out.println("Attempting to save transaction with user ID " + currentUserId + ": " + newTransaction);

            // 4. Simpan ke database via DAO
            boolean success = transactionDAO.saveTransaction(newTransaction);

            // 5. Tampilkan hasil
            if (success) {
                showSuccessAlert("Berhasil", "Transaksi berhasil disimpan!");
                clearForm();
                closeForm();
            } else {
                showAlert("Error", "Gagal menyimpan transaksi ke database.");
            }

        } catch (Exception e) {
            System.err.println("Error saving transaction: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Terjadi kesalahan saat menyimpan transaksi: " + e.getMessage());
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
        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().getCategoriesId() == -1) {
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