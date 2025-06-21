package com.example.ismoney.controller;

import com.example.ismoney.model.Budget;
import com.example.ismoney.service.BudgetService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

public class BudgetFormController {

    @FXML private TextField categoryField;
    @FXML private TextField limitAmountField;
    @FXML private ComboBox<String> periodComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private CheckBox isActiveCheckBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label titleLabel;

    private BudgetService budgetService;
    private Budget editingBudget;

    @FXML
    public void initialize() {
        budgetService = new BudgetService();
        setupForm();
    }

    private void setupForm() {
        // Setup period combo box
        periodComboBox.setItems(FXCollections.observableArrayList(
                "monthly", "weekly", "yearly"
        ));
        periodComboBox.setValue("monthly");

        // Setup date pickers
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusMonths(1));

        // Set default active
        isActiveCheckBox.setSelected(true);

        // Setup period change listener untuk auto-set end date
        periodComboBox.setOnAction(e -> updateEndDate());
        startDatePicker.setOnAction(e -> updateEndDate());

        // Setup input validation
        limitAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                limitAmountField.setText(oldValue);
            }
        });
    }

    private void updateEndDate() {
        if (startDatePicker.getValue() != null && periodComboBox.getValue() != null) {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate;

            switch (periodComboBox.getValue()) {
                case "weekly":
                    endDate = startDate.plusWeeks(1).minusDays(1);
                    break;
                case "monthly":
                    endDate = startDate.plusMonths(1).minusDays(1);
                    break;
                case "yearly":
                    endDate = startDate.plusYears(1).minusDays(1);
                    break;
                default:
                    endDate = startDate.plusMonths(1);
            }

            endDatePicker.setValue(endDate);
        }
    }

    public void setBudget(Budget budget) {
        this.editingBudget = budget;

        if (budget != null) {
            titleLabel.setText("Edit Budget");
            categoryField.setText(budget.getCategory());
            limitAmountField.setText(String.valueOf(budget.getLimitAmount()));
            periodComboBox.setValue(budget.getPeriod());
            startDatePicker.setValue(budget.getStartDate());
            endDatePicker.setValue(budget.getEndDate());
            isActiveCheckBox.setSelected(budget.isActive());
        } else {
            titleLabel.setText("Tambah Budget Baru");
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Budget budget;

            if (editingBudget != null) {
                // Update existing budget
                budget = editingBudget;
                budget.setCategory(categoryField.getText().trim());
                budget.setLimitAmount(Double.parseDouble(limitAmountField.getText()));
                budget.setPeriod(periodComboBox.getValue());
                budget.setStartDate(startDatePicker.getValue());
                budget.setEndDate(endDatePicker.getValue());
                budget.setActive(isActiveCheckBox.isSelected());

                budgetService.updateBudget(budget);
                showAlert("Sukses", "Budget berhasil diperbarui!");
            } else {
                // Create new budget
                budget = new Budget(
                        categoryField.getText().trim(),
                        Double.parseDouble(limitAmountField.getText()),
                        startDatePicker.getValue(),
                        endDatePicker.getValue(),
                        periodComboBox.getValue()
                );
                budget.setActive(isActiveCheckBox.isSelected());

                budgetService.saveBudget(budget);
                showAlert("Sukses", "Budget berhasil ditambahkan!");
            }

            closeWindow();

        } catch (SQLException e) {
            showAlert("Error", "Gagal menyimpan budget: " + e.getMessage());
        } catch (NumberFormatException e) {
            showAlert("Error", "Format angka tidak valid!");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Validate category
        if (categoryField.getText().trim().isEmpty()) {
            errors.append("- Kategori tidak boleh kosong\n");
        }

        // Validate limit amount
        if (limitAmountField.getText().trim().isEmpty()) {
            errors.append("- Batas anggaran tidak boleh kosong\n");
        } else {
            try {
                double limit = Double.parseDouble(limitAmountField.getText());
                if (limit <= 0) {
                    errors.append("- Batas anggaran harus lebih dari 0\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- Format batas anggaran tidak valid\n");
            }
        }

        // Validate dates
        if (startDatePicker.getValue() == null) {
            errors.append("- Tanggal mulai tidak boleh kosong\n");
        }

        if (endDatePicker.getValue() == null) {
            errors.append("- Tanggal berakhir tidak boleh kosong\n");
        }

        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                errors.append("- Tanggal berakhir tidak boleh lebih awal dari tanggal mulai\n");
            }
        }

        // Validate period selection
        if (periodComboBox.getValue() == null) {
            errors.append("- Periode tidak boleh kosong\n");
        }

        if (errors.length() > 0) {
            showAlert("Validasi Error", "Mohon perbaiki kesalahan berikut:\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}