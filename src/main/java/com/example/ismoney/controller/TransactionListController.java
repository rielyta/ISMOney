package com.example.ismoney.controller;

import com.example.ismoney.dao.CategoryDAO;
import com.example.ismoney.dao.TransactionDAO;
import com.example.ismoney.model.Category;
import com.example.ismoney.model.Transaction;
import com.example.ismoney.model.TransactionType;
import com.example.ismoney.util.SceneSwitcher;
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
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    @FXML private Button backButton;

    private TransactionDAO transactionDAO;
    private CategoryDAO categoryDAO;
    private ObservableList<Transaction> allTransactions;
    private Integer currentUserId;
    private Map<Integer, Category> categoryCache = new HashMap<>(); // Cache categories to reduce DB calls

    @FXML
    public void initialize() {
        System.out.println("TransactionListController initialized!");

        try {
            transactionDAO = new TransactionDAO();
            categoryDAO = new CategoryDAO();

            currentUserId = getCurrentLoggedInUserId();
            System.out.println("Using user ID for transaction list: " + currentUserId);

            setupTableColumns();
            setupFilterControls();
            setupTableSelectionListener();
            setupChart();
            loadCategoriesCache();
            loadTransactions();
            updateChart();

            System.out.println("TransactionListController setup completed!");
        } catch (Exception e) {
            System.err.println("Error initializing TransactionListController: " + e.getMessage());
            e.printStackTrace();
            showAlert("Kesalahan", "Gagal menginisialisasi controller: " + e.getMessage());
        }
    }

    private Integer getCurrentLoggedInUserId() {
        try {
            // Baca dari system properties (user yang sedang login)
            String currentUserIdStr = System.getProperty("current.user.id");
            if (currentUserIdStr != null && !currentUserIdStr.trim().isEmpty()) {
                try {
                    Integer currentUserId = Integer.valueOf(currentUserIdStr.trim());
                    System.out.println("Using logged-in user ID from system property: " + currentUserId);
                    return currentUserId;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid user ID in system property: " + currentUserIdStr);
                }
            }

            //  Redirect ke login jika tidak ada user yang login
            System.out.println("No logged-in user found. Should redirect to login.");

            // Untuk development/testing: gunakan user ID tertentu
            System.out.println("Using development user ID 1 for testing");
            return 1;

        } catch (Exception e) {
            System.err.println("Error getting current user ID: " + e.getMessage());
            return 1; // Development fallback
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

    private void loadCategoriesCache() {
        try {
            List<Category> categories = categoryDAO.getAllCategories();
            categoryCache.clear();
            for (Category category : categories) {
                categoryCache.put(category.getCategoriesId(), category);
            }
            System.out.println("Loaded " + categories.size() + " categories to cache");
        } catch (Exception e) {
            System.err.println("Error loading categories cache: " + e.getMessage());
        }
    }

    private String getCategoryNameFromCache(Integer categoryId) {
        Category category = categoryCache.get(categoryId);
        return category != null ? category.getName() : "Unknown";
    }

    private void setupChart() {
        lineChart.setTitle("Belum Ada Data Transaksi");
        lineChart.setLegendVisible(true);
        lineChart.setCreateSymbols(true);
        lineChart.setAnimated(true);

        lineChart.getXAxis().setLabel("");
        lineChart.getYAxis().setLabel("");
        lineChart.getXAxis().setTickLabelsVisible(true);
        lineChart.getYAxis().setTickLabelsVisible(true);

        lineChart.setHorizontalGridLinesVisible(false);
        lineChart.setVerticalGridLinesVisible(false);
        lineChart.setHorizontalZeroLineVisible(false);
        lineChart.setVerticalZeroLineVisible(false);
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

        categoryColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(getCategoryNameFromCache(cellData.getValue().getCategoryId())));

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

            if (transactions.isEmpty()) {
                System.out.println("No transactions found for user ID " + currentUserId);
                checkAllTransactions();
            } else {
                for (int i = 0; i < Math.min(3, transactions.size()); i++) {
                    Transaction t = transactions.get(i);
                    System.out.println("Transaction " + (i+1) + ": ID=" + t.getTransactionId() +
                            ", Amount=" + t.getAmount() +
                            ", Type=" + t.getType() +
                            ", Category=" + t.getCategoryId() +
                            ", Date=" + t.getTransactionDate());
                }
                if (transactions.size() > 3) {
                    System.out.println("... and " + (transactions.size() - 3) + " more transactions");
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading transactions: " + e.getMessage());
            e.printStackTrace();
            showAlert("Kesalahan", "Gagal memuat data transaksi: " + e.getMessage());
        }
    }

    private void checkAllTransactions() {
        try (Connection conn = com.example.ismoney.database.DatabaseConfig.getInstance().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT user_id, COUNT(*) as count FROM transactions GROUP BY user_id ORDER BY user_id");
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
                lineChart.setTitle("Belum Ada Data Transaksi");
                return;
            }

            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusMonths(11).withDayOfMonth(1);

            List<String> monthLabels = new ArrayList<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                monthLabels.add(current.format(DateTimeFormatter.ofPattern("MMM")));
                current = current.plusMonths(1);
            }

            Map<String, BigDecimal> monthlyIncome = allTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .filter(t -> !t.getTransactionDate().isBefore(startDate) && !t.getTransactionDate().isAfter(endDate))
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionDate().format(DateTimeFormatter.ofPattern("MMM")),
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            Map<String, BigDecimal> monthlyExpense = allTransactions.stream()
                    .filter(t -> t.getType() == TransactionType.OUTCOME)
                    .filter(t -> !t.getTransactionDate().isBefore(startDate) && !t.getTransactionDate().isAfter(endDate))
                    .collect(Collectors.groupingBy(
                            t -> t.getTransactionDate().format(DateTimeFormatter.ofPattern("MMM")),
                            Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                    ));

            lineChart.setTitle("Data Transaksi");

            XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
            incomeSeries.setName("Pemasukan");

            XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
            expenseSeries.setName("Pengeluaran");

            for (String month : monthLabels) {
                BigDecimal income = monthlyIncome.getOrDefault(month, BigDecimal.ZERO);
                BigDecimal expense = monthlyExpense.getOrDefault(month, BigDecimal.ZERO);

                incomeSeries.getData().add(new XYChart.Data<>(month, income.divide(BigDecimal.valueOf(1000))));
                expenseSeries.getData().add(new XYChart.Data<>(month, expense.divide(BigDecimal.valueOf(1000))));
            }

            lineChart.getData().clear();
            lineChart.getData().addAll(incomeSeries, expenseSeries);

            styleChartLines();

        } catch (Exception e) {
            System.err.println("Error updating chart: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void styleChartLines() {

        lineChart.applyCss();
        lineChart.layout();

        if (!lineChart.getData().isEmpty()) {
            lineChart.getData().get(0).getNode().setStyle("-fx-stroke: #4A90E2; -fx-stroke-width: 3px;");

            if (lineChart.getData().size() > 1) {
                // Style the second series (green line)
                lineChart.getData().get(1).getNode().setStyle("-fx-stroke: #7ED321; -fx-stroke-width: 3px;");
            }
        }
    }

    @FXML
    private void handleFilter() {
        if (allTransactions == null || allTransactions.isEmpty()) {
            return;
        }

        String filterValue = filterComboBox.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        List<Transaction> filteredTransactions = allTransactions.stream()
                .filter(transaction -> {
                    if (!filterValue.equals("Semua")) {
                        TransactionType filterType = filterValue.equals("Pemasukan") ?
                                TransactionType.INCOME : TransactionType.OUTCOME;
                        if (transaction.getType() != filterType) {
                            return false;
                        }
                    }

                    if (!searchText.isEmpty()) {
                        String categoryName = getCategoryNameFromCache(transaction.getCategoryId()).toLowerCase();
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
    private void handleAddTransaction() {
        try {
            SceneSwitcher.switchTo("Transaction/TransactionForm.fxml", (Stage) addTransactionButton.getScene().getWindow());
            loadCategoriesCache();
            loadTransactions();
            updateChart();

        } catch (Exception e) {
            System.err.println("Error opening transaction form: " + e.getMessage());
            e.printStackTrace();
            showAlert("Kesalahan", "Gagal membuka form transaksi: " + e.getMessage());
        }
    }



    @FXML
    private void handleEdit() {
        Transaction selectedTransaction = transactionTable.getSelectionModel().getSelectedItem();
        if (selectedTransaction != null) {
            try {
                System.out.println("=== DEBUG: Opening edit form ===");
                System.out.println("Selected transaction ID: " + selectedTransaction.getTransactionId());
                System.out.println("Selected transaction amount: " + selectedTransaction.getAmount());
                System.out.println("Selected transaction type: " + selectedTransaction.getType());
                System.out.println("Selected transaction category ID: " + selectedTransaction.getCategoryId());


                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ismoney/Transaction/TransactionEditForm.fxml"));
                Parent root = loader.load();

                TransactionEditFormController editController = loader.getController();

                editController.setTransaction(selectedTransaction);

                Stage currentStage = (Stage) editButton.getScene().getWindow();
                Scene editScene = new Scene(root);
                currentStage.setScene(editScene);
                currentStage.setTitle("ISMoney - Edit Transaksi");

                System.out.println("Successfully opened edit form with transaction data");

            } catch (Exception e) {
                System.err.println("Error opening edit form: " + e.getMessage());
                e.printStackTrace();
                showAlert("Kesalahan", "Gagal membuka form edit: " + e.getMessage());
            }
        } else {
            showAlert("Peringatan", "Pilih transaksi yang akan diedit terlebih dahulu.");
        }
    }

    @FXML
    private void handleDelete() {
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

    @FXML
    private void backTo(){
        SceneSwitcher.switchTo("Dashboard.fxml", (Stage) backButton.getScene().getWindow());
    }
}