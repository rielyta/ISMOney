package com.example.ismoney.controller;

import com.example.ismoney.model.Budget;
import com.example.ismoney.service.BudgetService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    @FXML private Button refreshBtn;
    @FXML private ComboBox<String> filterPeriodBox;
    @FXML private CheckBox showActiveOnlyBox;

    @FXML private PieChart budgetChart;
    @FXML private Label totalBudgetLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label remainingBudgetLabel;

    private BudgetService budgetService;
    private ObservableList<Budget> budgetList;
    private NumberFormat currencyFormat;

    @FXML
    public void initialize() {
        budgetService = new BudgetService();
        budgetList = FXCollections.observableArrayList();
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        setupTable();
        setupChart();
        setupFilters();
        loadBudgets();
    }

    private void setupTable() {
        // Setup columns
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        limitColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(currencyFormat.format(cellData.getValue().getLimitAmount())));

        spentColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(currencyFormat.format(cellData.getValue().getSpentAmount())));

        remainingColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(currencyFormat.format(cellData.getValue().getRemainingAmount())));

        periodColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPeriod().toUpperCase()));

        // Status column with color coding
        statusColumn.setCellValueFactory(cellData -> {
            Budget budget = cellData.getValue();
            String status = getStatusText(budget);
            return new SimpleStringProperty(status);
        });

        statusColumn.setCellFactory(column -> new TableCell<Budget, String>() {
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

        // Action column with buttons
        actionColumn.setCellFactory(new Callback<TableColumn<Budget, Void>, TableCell<Budget, Void>>() {
            @Override
            public TableCell<Budget, Void> call(TableColumn<Budget, Void> param) {
                return new TableCell<Budget, Void>() {
                    private final Button editBtn = new Button("Edit");
                    private final Button deleteBtn = new Button("Hapus");
                    private final HBox buttons = new HBox(5);

                    {
                        editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-pref-width: 60;");
                        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-pref-width: 60;");

                        editBtn.setOnAction(e -> {
                            Budget budget = getTableView().getItems().get(getIndex());
                            editBudget(budget);
                        });

                        deleteBtn.setOnAction(e -> {
                            Budget budget = getTableView().getItems().get(getIndex());
                            deleteBudget(budget);
                        });

                        buttons.getChildren().addAll(editBtn, deleteBtn);
                        buttons.setAlignment(Pos.CENTER);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttons);
                        }
                    }
                };
            }
        });

        budgetTable.setItems(budgetList);
        budgetTable.setRowFactory(tv -> {
            TableRow<Budget> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldBudget, newBudget) -> {
                if (newBudget != null) {
                    if (newBudget.isOverBudget()) {
                        row.setStyle("-fx-background-color: #ffebee;");
                    } else if (newBudget.getUsagePercentage() >= 80) {
                        row.setStyle("-fx-background-color: #fff3e0;");
                    } else {
                        row.setStyle("");
                    }
                }
            });
            return row;
        });
    }

    private void setupChart() {
        budgetChart.setTitle("Budget Usage Overview");
        budgetChart.setLegendVisible(true);
    }

    private void setupFilters() {
        filterPeriodBox.setItems(FXCollections.observableArrayList(
                "Semua", "Monthly", "Weekly", "Yearly"
        ));
        filterPeriodBox.setValue("Semua");

        filterPeriodBox.setOnAction(e -> applyFilters());
        showActiveOnlyBox.setOnAction(e -> applyFilters());
    }

    @FXML
    private void handleAddBudget() {
        openBudgetForm(null);
    }

    @FXML
    private void handleRefresh() {
        loadBudgets();
    }

    private void loadBudgets() {
        try {
            List<Budget> budgets = budgetService.getAllBudgets();
            budgetList.clear();
            budgetList.addAll(budgets);
            updateSummary();
            updateChart();
            applyFilters();
        } catch (SQLException e) {
            showAlert("Error", "Gagal memuat data budget: " + e.getMessage());
        }
    }

    private void applyFilters() {
        ObservableList<Budget> filteredList = FXCollections.observableArrayList();

        for (Budget budget : budgetList) {
            boolean periodMatch = filterPeriodBox.getValue().equals("Semua") ||
                    budget.getPeriod().equalsIgnoreCase(filterPeriodBox.getValue());

            boolean activeMatch = !showActiveOnlyBox.isSelected() || budget.isActive();

            if (periodMatch && activeMatch) {
                filteredList.add(budget);
            }
        }

        budgetTable.setItems(filteredList);
    }

    private void updateSummary() {
        double totalLimit = budgetList.stream()
                .filter(Budget::isActive)
                .mapToDouble(Budget::getLimitAmount)
                .sum();

        double totalSpent = budgetList.stream()
                .filter(Budget::isActive)
                .mapToDouble(Budget::getSpentAmount)
                .sum();

        double remaining = totalLimit - totalSpent;

        totalBudgetLabel.setText(currencyFormat.format(totalLimit));
        totalSpentLabel.setText(currencyFormat.format(totalSpent));
        remainingBudgetLabel.setText(currencyFormat.format(remaining));

        // Set color based on status
        if (remaining < 0) {
            remainingBudgetLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else if (remaining < totalLimit * 0.2) {
            remainingBudgetLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
        } else {
            remainingBudgetLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        }
    }

    private void updateChart() {
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        for (Budget budget : budgetList) {
            if (budget.isActive() && budget.getSpentAmount() > 0) {
                chartData.add(new PieChart.Data(
                        budget.getCategory(),
                        budget.getSpentAmount()
                ));
            }
        }

        budgetChart.setData(chartData);
    }

    private void openBudgetForm(Budget budget) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/ismoney/budget/BudgetForm.fxml"));
            Parent root = loader.load();

            BudgetFormController controller = loader.getController();
            if (budget != null) {
                controller.setBudget(budget);
            }

            Stage stage = new Stage();
            stage.setTitle(budget == null ? "Tambah Budget Baru" : "Edit Budget");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh setelah form ditutup
            loadBudgets();

        } catch (IOException e) {
            showAlert("Error", "Gagal membuka form budget: " + e.getMessage());
        }
    }

    private void editBudget(Budget budget) {
        openBudgetForm(budget);
    }

    private void deleteBudget(Budget budget) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus Budget");
        alert.setContentText("Apakah Anda yakin ingin menghapus budget untuk kategori \"" +
                budget.getCategory() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                budgetService.deleteBudget(budget.getId());
                loadBudgets();
                showAlert("Sukses", "Budget berhasil dihapus!");
            } catch (SQLException e) {
                showAlert("Error", "Gagal menghapus budget: " + e.getMessage());
            }
        }
    }

    private String getStatusText(Budget budget) {
        if (!budget.isActive()) {
            return "INACTIVE";
        }

        double percentage = budget.getUsagePercentage();
        if (budget.isOverBudget()) {
            return String.format("OVER (%.1f%%)", percentage);
        } else if (percentage >= 90) {
            return String.format("CRITICAL (%.1f%%)", percentage);
        } else if (percentage >= 70) {
            return String.format("WARNING (%.1f%%)", percentage);
        } else {
            return String.format("HEALTHY (%.1f%%)", percentage);
        }
    }

    private String getStatusStyle(Budget budget) {
        if (!budget.isActive()) {
            return "-fx-text-fill: gray;";
        }

        if (budget.isOverBudget()) {
            return "-fx-text-fill: red; -fx-font-weight: bold;";
        } else if (budget.getUsagePercentage() >= 90) {
            return "-fx-text-fill: red;";
        } else if (budget.getUsagePercentage() >= 70) {
            return "-fx-text-fill: orange;";
        } else {
            return "-fx-text-fill: green;";
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}