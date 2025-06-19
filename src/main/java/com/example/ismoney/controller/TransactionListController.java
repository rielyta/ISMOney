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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionListController {

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, Integer> idColumn;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, String> categoryColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, String> noteColumn;

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<Category> categoryFilterComboBox;
    @FXML private TextField searchField;
    @FXML private Button filterButton;
    @FXML private Button clearFilterButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    private TransactionDAO transactionDAO;
    private CategoryDAO categoryDAO;
    private ObservableList<Transaction> allTransactions;
    private static final Integer CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        transactionDAO = new TransactionDAO();
        categoryDAO = new CategoryDAO();

        setupTableColumns();
        setupFilterControls();
        loadTransactions();
        setupTableSelectionListener();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("transactionId"));

        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTransactionDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getType().toString()));

        categoryColumn.setCellValueFactory(cellData -> {
            Category category = categoryDAO.getCategoryById(cellData.getValue().getCategoryId());
            return new SimpleStringProperty(category != null ? category.getName() : "Unknown");
        });

        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        // Format amount column to show currency
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

        typeColumn.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(type);
                    if (type.equals("INCOME")) {
                        setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupFilterControls() {
        typeFilterComboBox.setItems(FXCollections.observableArrayList("ALL", "INCOME", "OUTCOME"));
        typeFilterComboBox.setValue("ALL");

        List<Category> categories = categoryDAO.getAllCategories();
        Category allCategory = new Category(-1, "ALL", "ALL");
        categories.add(0, allCategory);
        categoryFilterComboBox.setItems(FXCollections.observableArrayList(categories));
        categoryFilterComboBox.setValue(allCategory);

        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
    }

    private void setupTableSelectionListener() {
        transactionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            editButton.setDisable(newSelection == null);
            deleteButton.setDisable(newSelection == null);
        });
    }

    private void loadTransactions() {
        List<Transaction> transactions = transactionDAO.getTransactionsByUserId(CURRENT_USER_ID);
        allTransactions = FXCollections.observableArrayList(transactions);
        transactionTable.setItems(allTransactions);
    }

    @FXML
    private void handleFilter() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        String typeFilter = typeFilterComboBox.getValue();
        Category categoryFilter = categoryFilterComboBox.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        List<Transaction> filteredTransactions = allTransactions.stream()
                .filter(transaction -> {
                    // Date filter
                    if (startDate != null && transaction.getTransactionDate().isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && transaction.getTransactionDate().isAfter(endDate)) {
                        return false;
                    }

                    // Type filter
                    if (!typeFilter.equals("ALL") && !transaction.getType().toString().equals(typeFilter)) {
                        return false;
                    }

                    // Category filter
                    if (categoryFilter.getCategoriesId() != -1 &&
                            !transaction.getCategoryId().equals(categoryFilter.getCategoriesId())) {
                        return false;
                    }

                    // Search filter (note and category name)
                    if (!searchText.isEmpty()) {
                        Category category = categoryDAO.getCategoryById(transaction.getCategoryId());
                        String categoryName = category != null ? category.getName().toLowerCase() : "";
                        String note = transaction.getNote() != null ? transaction.getNote().toLowerCase() : "";

                        if (!categoryName.contains(searchText) && !note.contains(searchText)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        transactionTable.setItems(FXCollections.observableArrayList(filteredTransactions));
    }

    @FXML
    private void handleClearFilter() {
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        endDatePicker.setValue(LocalDate.now());
        typeFilterComboBox.setValue("ALL");
        categoryFilterComboBox.setValue(categoryFilterComboBox.getItems().get(0));
        searchField.clear();
        transactionTable.setItems(allTransactions);
    }

    @FXML
    private void handleEdit() {
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null) {
            // TODO: Open edit form with selected transaction data
            System.out.println("Edit transaction: " + selectedTransaction.getTransactionId());
        }
    }

    @FXML
    private void handleDelete() {
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Konfirmasi Hapus");
            confirmAlert.setHeaderText("Hapus Transaksi");
            confirmAlert.setContentText("Apakah Anda yakin ingin menghapus transaksi ini?");

            if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                boolean success = transactionDAO.deleteTransaction(selectedTransaction.getTransactionId());
                if (success) {
                    loadTransactions(); // Reload table
                    showSuccessAlert("Berhasil", "Transaksi berhasil dihapus!");
                } else {
                    showAlert("Error", "Gagal menghapus transaksi.");
                }
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadTransactions();
        handleClearFilter();
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