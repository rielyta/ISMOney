package com.example.ismoney.controller;

import com.example.ismoney.dao.CategoryDAO;
import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.model.Category;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionListController {

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, BigDecimal> incomeColumn;
    @FXML private TableColumn<Transaction, BigDecimal> expenseColumn;
    @FXML private TableColumn<Transaction, String> categoryColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button searchButton;
    @FXML private Button addTransactionButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button refreshButton;

    private TransactionDAO transactionDAO;
    private CategoryDAO categoryDAO;
    private ObservableList<Transaction> allTransactions;
    private static final Integer CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        System.out.println("TransactionListController initialized!"); // Debug line

        try {
            transactionDAO = new TransactionDAO();
            categoryDAO = new CategoryDAO();

            setupTableColumns();
            setupFilterControls();
            loadTransactions();
            setupTableSelectionListener();

            System.out.println("TransactionListController setup completed!"); // Debug line
        } catch (Exception e) {
            System.err.println("Error initializing TransactionListController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        // Setup kolom tanggal
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTransactionDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        // Setup kolom pemasukan (hanya tampilkan jika INCOME)
        incomeColumn.setCellValueFactory(cellData -> {
            Transaction transaction = cellData.getValue();
            if (transaction.getType() == TransactionType.INCOME) {
                return new javafx.beans.property.SimpleObjectProperty<>(transaction.getAmount());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });

        // Setup kolom pengeluaran (hanya tampilkan jika OUTCOME)
        expenseColumn.setCellValueFactory(cellData -> {
            Transaction transaction = cellData.getValue();
            if (transaction.getType() == TransactionType.OUTCOME) {
                return new javafx.beans.property.SimpleObjectProperty<>(transaction.getAmount());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });

        // Setup kolom kategori
        categoryColumn.setCellValueFactory(cellData -> {
            try {
                Category category = categoryDAO.getCategoryById(cellData.getValue().getCategoryId());
                return new SimpleStringProperty(category != null ? category.getName() : "Unknown");
            } catch (Exception e) {
                return new SimpleStringProperty("Unknown");
            }
        });

        // Setup kolom total
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Format kolom currency
        formatCurrencyColumns();
    }

    private void formatCurrencyColumns() {
        incomeColumn.setCellFactory(column -> new TableCell<Transaction, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText("Rp " + String.format("%,.0f", amount.doubleValue()));
                    setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            }
        });

        expenseColumn.setCellFactory(column -> new TableCell<Transaction, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText("Rp " + String.format("%,.0f", amount.doubleValue()));
                    setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            }
        });

        amountColumn.setCellFactory(column -> new TableCell<Transaction, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                } else {
                    setText("Rp " + String.format("%,.0f", amount.doubleValue()));
                }
            }
        });
    }

    private void setupFilterControls() {
        filterComboBox.setItems(FXCollections.observableArrayList("Semua", "Pemasukan", "Pengeluaran"));
        filterComboBox.setValue("Semua");
    }

    private void setupTableSelectionListener() {
        transactionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            editButton.setDisable(newSelection == null);
            deleteButton.setDisable(newSelection == null);
        });
    }

    private void loadTransactions() {
        try {
            List<Transaction> transactions = transactionDAO.getTransactionsByUserId(CURRENT_USER_ID);
            allTransactions = FXCollections.observableArrayList(transactions);
            transactionTable.setItems(allTransactions);
            System.out.println("Loaded " + transactions.size() + " transactions"); // Debug line
        } catch (Exception e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            showAlert("Kesalahan", "Gagal memuat data transaksi: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter() {
        System.out.println("Filter button clicked!"); // Debug line
        String filterValue = filterComboBox.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        List<Transaction> filteredTransactions = allTransactions.stream()
                .filter(transaction -> {
                    // Filter berdasarkan tipe
                    if (!filterValue.equals("Semua")) {
                        TransactionType filterType = filterValue.equals("Pemasukan") ?
                                TransactionType.INCOME : TransactionType.OUTCOME;
                        if (transaction.getType() != filterType) {
                            return false;
                        }
                    }

                    // Filter berdasarkan pencarian
                    if (!searchText.isEmpty()) {
                        try {
                            Category category = categoryDAO.getCategoryById(transaction.getCategoryId());
                            String categoryName = category != null ? category.getName().toLowerCase() : "";
                            String note = transaction.getNote() != null ? transaction.getNote().toLowerCase() : "";

                            if (!categoryName.contains(searchText) && !note.contains(searchText)) {
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        transactionTable.setItems(FXCollections.observableArrayList(filteredTransactions));
    }

    @FXML
    private void handleAddTransaction() {
        System.out.println("Add Transaction button clicked!"); // Debug line
        try {
            // Load FXML file untuk form transaksi
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ismoney/Transaction/TransactionForm.fxml"));
            Parent root = loader.load();

            // Buat stage baru untuk form transaksi
            Stage transactionStage = new Stage();
            transactionStage.setTitle("Tambah Transaksi");
            transactionStage.setScene(new Scene(root));

            // Set modality agar user harus menutup window ini sebelum kembali ke main window
            transactionStage.initModality(Modality.APPLICATION_MODAL);

            // Tampilkan form transaksi dan tunggu sampai ditutup
            transactionStage.showAndWait();

            // Refresh tabel setelah form ditutup
            loadTransactions();

        } catch (IOException e) {
            showAlert("Kesalahan", "Gagal membuka form transaksi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEdit() {
        System.out.println("Edit button clicked!"); // Debug line
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null) {
            showAlert("Info", "Fitur edit akan segera tersedia!");
        }
    }

    @FXML
    private void handleDelete() {
        System.out.println("Delete button clicked!"); // Debug line
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Konfirmasi Hapus");
            confirmAlert.setHeaderText("Hapus Transaksi");
            confirmAlert.setContentText("Apakah Anda yakin ingin menghapus transaksi ini?");

            if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                try {
                    boolean success = transactionDAO.deleteTransaction(selectedTransaction.getTransactionId());
                    if (success) {
                        loadTransactions(); // Reload tabel
                        showSuccessAlert("Berhasil", "Transaksi berhasil dihapus!");
                    } else {
                        showAlert("Kesalahan", "Gagal menghapus transaksi.");
                    }
                } catch (Exception e) {
                    showAlert("Kesalahan", "Terjadi kesalahan saat menghapus: " + e.getMessage());
                }
            }
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