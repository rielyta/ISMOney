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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransactionListController {

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, BigDecimal> incomeColumn;
    @FXML private TableColumn<Transaction, BigDecimal> expenseColumn;
    @FXML private TableColumn<Transaction, String> categoryColumn;
    @FXML private TableColumn<Transaction, BigDecimal> totalColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Button searchButton;
    @FXML private Button addTransactionButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private LineChart<String, Number> lineChart;

    // Summary labels
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;
    @FXML private Label balanceLabel;

    private TransactionDAO transactionDAO;
    private CategoryDAO categoryDAO;
    private ObservableList<Transaction> allTransactions;
    private Integer currentUserId;

    @FXML
    public void initialize() {
        System.out.println("TransactionListController initialized!");

        try {
            transactionDAO = new TransactionDAO();
            categoryDAO = new CategoryDAO();

            // Get dynamic user ID
            currentUserId = getValidUserId();
            System.out.println("Using user ID for transaction list: " + currentUserId);

            setupTableColumns();
            setupFilterControls();
            setupTableSelectionListener();
            loadTransactions();
            updateChart();


            System.out.println("TransactionListController setup completed!");
        } catch (Exception e) {
            System.err.println("Error initializing TransactionListController: " + e.getMessage());
            e.printStackTrace();
            showAlert("Kesalahan", "Gagal menginisialisasi controller: " + e.getMessage());
        }
    }

    private Integer getValidUserId() {
        try {
            Integer existingUserId = getFirstExistingUserId();
            if (existingUserId != null) {
                return existingUserId;
            }
            return 1;
        } catch (Exception e) {
            System.err.println("Error getting valid user ID: " + e.getMessage());
            return 1;
        }
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

    private void setupTableColumns() {
        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTransactionDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        incomeColumn.setCellValueFactory(cellData -> {
            Transaction transaction = cellData.getValue();
            if (transaction.getType() == TransactionType.INCOME) {
                return new javafx.beans.property.SimpleObjectProperty<>(transaction.getAmount());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });

        expenseColumn.setCellValueFactory(cellData -> {
            Transaction transaction = cellData.getValue();
            if (transaction.getType() == TransactionType.OUTCOME) {
                return new javafx.beans.property.SimpleObjectProperty<>(transaction.getAmount());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });

        categoryColumn.setCellValueFactory(cellData -> {
            try {
                Category category = categoryDAO.getCategoryById(cellData.getValue().getCategoryId());
                return new SimpleStringProperty(category != null ? category.getName() : "Unknown");
            } catch (Exception e) {
                System.err.println("Error getting category: " + e.getMessage());
                return new SimpleStringProperty("Unknown");
            }
        });

        totalColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
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

        totalColumn.setCellFactory(column -> new TableCell<Transaction, BigDecimal>() {
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

        editButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void loadTransactions() {
        try {
            System.out.println("=== DEBUG: Loading transactions ===");
            System.out.println("Loading transactions for user ID: " + currentUserId);

            List<Transaction> transactions = transactionDAO.getTransactionsByUserId(currentUserId);
            allTransactions = FXCollections.observableArrayList(transactions);
            transactionTable.setItems(allTransactions);

            System.out.println("Loaded " + transactions.size() + " transactions");

            for (Transaction t : transactions) {
                System.out.println("Transaction: ID=" + t.getTransactionId() +
                        ", Amount=" + t.getAmount() +
                        ", Type=" + t.getType() +
                        ", Category=" + t.getCategoryId() +
                        ", Date=" + t.getTransactionDate());
            }

            if (transactions.isEmpty()) {
                System.out.println("No transactions found for user ID " + currentUserId);
                checkAllTransactions();
            }

        } catch (Exception e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
            showAlert("Kesalahan", "Gagal memuat data transaksi: " + e.getMessage());
        }
    }

    private void checkAllTransactions() {
        try (Connection conn = com.example.ismoney.database.DatabaseConfig.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT user_id, COUNT(*) as count FROM transactions GROUP BY user_id");
            ResultSet rs = stmt.executeQuery();

            System.out.println("=== All transactions by user ID ===");
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                System.out.println("User ID " + rs.getInt("user_id") + " has " + rs.getInt("count") + " transactions");
            }

            if (!hasData) {
                System.out.println("No transactions found in database at all!");
            }

        } catch (Exception e) {
            System.err.println("Error checking all transactions: " + e.getMessage());
        }
    }

    private void updateChart() {
        try {
            if (allTransactions == null || allTransactions.isEmpty()) {
                lineChart.getData().clear();
                return;
            }

            // Group transactions by date
            Map<LocalDate, BigDecimal> dailyIncome = allTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .collect(Collectors.groupingBy(
                            Transaction::getTransactionDate,
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            Map<LocalDate, BigDecimal> dailyExpense = allTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.OUTCOME)
                    .collect(Collectors.groupingBy(
                            Transaction::getTransactionDate,
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            // Create series for chart
            XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
            incomeSeries.setName("Pemasukan");

            XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
            expenseSeries.setName("Pengeluaran");

            // Add data to series
            dailyIncome.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        String dateStr = entry.getKey().format(DateTimeFormatter.ofPattern("dd/MM"));
                        incomeSeries.getData().add(new XYChart.Data<>(dateStr, entry.getValue()));
                    });

            dailyExpense.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        String dateStr = entry.getKey().format(DateTimeFormatter.ofPattern("dd/MM"));
                        expenseSeries.getData().add(new XYChart.Data<>(dateStr, entry.getValue()));
                    });

            lineChart.getData().clear();
            lineChart.getData().addAll(incomeSeries, expenseSeries);

        } catch (Exception e) {
            System.err.println("Error updating chart: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    private void handleFilter() {
        System.out.println("Filter button clicked!");

        if (allTransactions == null || allTransactions.isEmpty()) {
            System.out.println("No transactions to filter");
            return;
        }

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
                            System.err.println("Error filtering transaction: " + e.getMessage());
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        transactionTable.setItems(FXCollections.observableArrayList(filteredTransactions));
        System.out.println("Filtered to " + filteredTransactions.size() + " transactions");
    }

    @FXML
    private void handleAddTransaction() {
        System.out.println("Add Transaction button clicked!");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ismoney/Transaction/TransactionForm.fxml"));
            Parent root = loader.load();

            Stage transactionStage = new Stage();
            transactionStage.setTitle("Tambah Transaksi");
            transactionStage.setScene(new Scene(root));
            transactionStage.initModality(Modality.APPLICATION_MODAL);

            transactionStage.showAndWait();

            loadTransactions();
            updateChart();


        } catch (IOException e) {
            System.err.println("Error opening transaction form: " + e.getMessage());
            e.printStackTrace();
            showAlert("Kesalahan", "Gagal membuka form transaksi: " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        System.out.println("Edit button clicked!");
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ismoney/Transaction/TransactionEditForm.fxml"));
                Parent root = loader.load();

                // Get controller and pass selected transaction
                TransactionEditFormController controller = loader.getController();
                controller.setTransaction(selectedTransaction);

                Stage editStage = new Stage();
                editStage.setTitle("Edit Transaksi");
                editStage.setScene(new Scene(root));
                editStage.initModality(Modality.APPLICATION_MODAL);

                editStage.showAndWait();

                loadTransactions();
                updateChart();


            } catch (IOException e) {
                System.err.println("Error opening edit form: " + e.getMessage());
                // Fallback message if edit form doesn't exist yet
                showAlert("Info", "Fitur edit dalam pengembangan. Silakan hapus dan buat ulang transaksi.");
            }
        }
    }

    @FXML
    private void handleDelete() {
        System.out.println("Delete button clicked!");
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Konfirmasi Hapus");
            confirmAlert.setHeaderText("Hapus Transaksi");
            confirmAlert.setContentText("Apakah Anda yakin ingin menghapus transaksi ini?\n\n" +
                    "Jumlah: Rp " + String.format("%,.0f", selectedTransaction.getAmount().doubleValue()) + "\n" +
                    "Tanggal: " + selectedTransaction.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    boolean success = transactionDAO.deleteTransaction(selectedTransaction.getTransactionId());
                    if (success) {
                        loadTransactions();
                        updateChart();

                        showSuccessAlert("Berhasil", "Transaksi berhasil dihapus!");
                    } else {
                        showAlert("Kesalahan", "Gagal menghapus transaksi.");
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting transaction: " + e.getMessage());
                    e.printStackTrace();
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