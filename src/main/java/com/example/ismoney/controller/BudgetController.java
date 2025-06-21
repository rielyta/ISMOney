package com.example.ismoney.controller;

import com.example.ismoney.database.DatabaseConfig;
import com.example.ismoney.model.Budget;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class BudgetController {

    @FXML private TableView<Budget> budgetTable;
    @FXML private TableColumn<Budget, String> categoryColumn;
    @FXML private TableColumn<Budget, String> limitColumn;
    @FXML private TableColumn<Budget, String> spentColumn;
    @FXML private TableColumn<Budget, String> remainingColumn;
    @FXML private TableColumn<Budget, String> periodColumn;
    @FXML private TableColumn<Budget, String> statusColumn;
    @FXML private TableColumn<Budget, Void> actionColumn;

    @FXML private Button addBudgetBtn;
    @FXML private ComboBox<String> filterPeriodBox;
    @FXML private CheckBox showActiveOnlyBox;

    @FXML private PieChart budgetChart;
    @FXML private Label totalBudgetLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label remainingBudgetLabel;

    private ObservableList<Budget> budgetList;
    private ObservableList<Budget> filteredBudgetList;
    private NumberFormat currencyFormat;

    @FXML
    public void initialize() {
        budgetList = FXCollections.observableArrayList();
        filteredBudgetList = FXCollections.observableArrayList();
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        setupTableColumns();
        setupChart();
        setupFilters();
        loadBudgetData();
        updateSummaryCards();
        updateChart();
    }

    private void setupTableColumns() {
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        limitColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatCurrency(cellData.getValue().getLimitAmount())));

        spentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatCurrency(cellData.getValue().getSpentAmount())));

        remainingColumn.setCellValueFactory(cellData -> {
            Budget budget = cellData.getValue();
            double remaining = budget.getRemainingAmount();
            return new SimpleStringProperty(formatCurrency(remaining));
        });

        periodColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPeriod().toUpperCase()));

        statusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(getStatusText(cellData.getValue())));

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Budget budget = getTableView().getItems().get(getIndex());
                    setStyle(getStatusStyle(budget));
                }
            }
        });

        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Hapus");
            private final HBox buttons = new HBox(5);

            {
                editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-pref-width: 60; -fx-font-size: 9px;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-pref-width: 60; -fx-font-size: 9px;");

                editBtn.setOnAction(e -> editBudget(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteBudget(getTableView().getItems().get(getIndex())));

                buttons.getChildren().addAll(editBtn, deleteBtn);
                buttons.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

        budgetTable.setItems(filteredBudgetList);
    }

    private void setupChart() {
        budgetChart.setTitle("Budget Usage Overview");
        budgetChart.setLegendVisible(true);
    }

    private void setupFilters() {
        filterPeriodBox.setItems(FXCollections.observableArrayList("Semua", "Harian", "Mingguan", "Bulanan", "Tahunan"));
        filterPeriodBox.setValue("Semua");

        filterPeriodBox.setOnAction(e -> applyFilters());
        showActiveOnlyBox.setOnAction(e -> applyFilters());
    }

    private void loadBudgetData() {
        budgetList.clear();
        String sql = "SELECT * FROM budgets ORDER BY created_at DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Budget budget = new Budget();
                budget.setId(rs.getInt("id"));
                budget.setCategory(rs.getString("category"));
                budget.setLimitAmount(rs.getDouble("budget_limit"));
                budget.setSpentAmount(rs.getDouble("spent"));
                budget.setPeriod(rs.getString("period"));
                budget.setActive(rs.getString("status").equalsIgnoreCase("Aktif"));
                budgetList.add(budget);
            }
            applyFilters();
        } catch (SQLException e) {
            System.err.println("Error loading budget data: " + e.getMessage());
            showErrorAlert("Gagal memuat data budget: " + e.getMessage());
        }
    }

    private void applyFilters() {
        filteredBudgetList.clear();
        for (Budget budget : budgetList) {
            boolean periodMatch = filterPeriodBox.getValue().equals("Semua") ||
                    budget.getPeriod().equalsIgnoreCase(filterPeriodBox.getValue());
            boolean activeMatch = !showActiveOnlyBox.isSelected() ||
                    budget.isActive();
            if (periodMatch && activeMatch) filteredBudgetList.add(budget);
        }
        budgetTable.setItems(filteredBudgetList);
    }

    private void updateSummaryCards() {
        double totalBudget = 0.0;
        double totalSpent = 0.0;

        for (Budget budget : filteredBudgetList) {
            if (budget.isActive()) {
                totalBudget += budget.getLimitAmount();
                totalSpent += budget.getSpentAmount();
            }
        }

        double remaining = totalBudget - totalSpent;

        totalBudgetLabel.setText(formatCurrency(totalBudget));
        totalSpentLabel.setText(formatCurrency(totalSpent));
        remainingBudgetLabel.setText(formatCurrency(remaining));

        if (remaining < 0) {
            remainingBudgetLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else if (remaining < totalBudget * 0.2) {
            remainingBudgetLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            remainingBudgetLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }

    private void updateChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Budget budget : filteredBudgetList) {
            if (budget.isActive() && budget.getSpentAmount() > 0) {
                pieChartData.add(new PieChart.Data(budget.getCategory(), budget.getSpentAmount()));
            }
        }
        budgetChart.setData(pieChartData);
    }

    @FXML
    private void handleAddBudget() {
        Dialog<Budget> dialog = new Dialog<>();
        dialog.setTitle("Tambah Budget");
        dialog.setHeaderText("Masukkan informasi budget baru");

        ButtonType addButtonType = new ButtonType("Tambah", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField categoryField = new TextField();
        categoryField.setPromptText("Kategori");

        TextField limitField = new TextField();
        limitField.setPromptText("Batas (tanpa Rp)");

        ComboBox<String> periodBox = new ComboBox<>();
        periodBox.getItems().addAll("Harian", "Mingguan", "Bulanan", "Tahunan");
        periodBox.setValue("Bulanan");

        grid.add(new Label("Kategori:"), 0, 0);
        grid.add(categoryField, 1, 0);
        grid.add(new Label("Batas Anggaran:"), 0, 1);
        grid.add(limitField, 1, 1);
        grid.add(new Label("Periode:"), 0, 2);
        grid.add(periodBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);

        categoryField.textProperty().addListener((obs, oldVal, newVal) -> {
            addButton.setDisable(newVal.trim().isEmpty() || limitField.getText().trim().isEmpty());
        });

        limitField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                limitField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            addButton.setDisable(newVal.trim().isEmpty() || categoryField.getText().trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String category = categoryField.getText().trim();
                    double limit = Double.parseDouble(limitField.getText().trim());
                    String period = periodBox.getValue();

                    Budget newBudget = new Budget();
                    newBudget.setCategory(category);
                    newBudget.setLimitAmount(limit);
                    newBudget.setSpentAmount(0.0);
                    newBudget.setPeriod(period);
                    newBudget.setActive(true);

                    return newBudget;
                } catch (NumberFormatException e) {
                    showErrorAlert("Input tidak valid!");
                }
            }
            return null;
        });

        Optional<Budget> result = dialog.showAndWait();
        result.ifPresent(budget -> {
            if (saveBudgetToDatabase(budget)) {
                showSuccessAlert("Budget berhasil ditambahkan!");
                loadBudgetData();
                updateSummaryCards();
                updateChart();
            } else {
                showErrorAlert("Gagal menyimpan budget ke database.");
            }
        });
    }

    private boolean saveBudgetToDatabase(Budget budget) {
        String sql = "INSERT INTO budgets (category, budget_limit, spent, period, status, created_at) VALUES (?, ?, ?, ?, ?, CURRENT_DATE)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, budget.getCategory());
            stmt.setDouble(2, budget.getLimitAmount());
            stmt.setDouble(3, budget.getSpentAmount());
            stmt.setString(4, budget.getPeriod());
            stmt.setString(5, budget.isActive() ? "Aktif" : "Tidak Aktif");
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void editBudget(Budget budget) { /* TODO */ }
    private void deleteBudget(Budget budget) { /* TODO */ }

    private String getStatusText(Budget budget) {
        if (!budget.isActive()) return "INACTIVE";
        double pct = budget.getUsagePercentage();
        if (budget.isOverBudget()) return String.format("OVER (%.1f%%)", pct);
        if (pct >= 90) return String.format("CRITICAL (%.1f%%)", pct);
        if (pct >= 70) return String.format("WARNING (%.1f%%)", pct);
        return String.format("HEALTHY (%.1f%%)", pct);
    }

    private String getStatusStyle(Budget budget) {
        if (!budget.isActive()) return "-fx-text-fill: gray;";
        if (budget.isOverBudget()) return "-fx-text-fill: red; -fx-font-weight: bold;";
        if (budget.getUsagePercentage() >= 90) return "-fx-text-fill: red;";
        if (budget.getUsagePercentage() >= 70) return "-fx-text-fill: orange;";
        return "-fx-text-fill: green;";
    }

    private String formatCurrency(double amount) {
        return String.format("Rp %,.0f", amount);
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sukses");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} // End of BudgetController