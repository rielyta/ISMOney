package com.example.ismoney.controller;

import com.example.ismoney.dao.UserDAOImpl;
import com.example.ismoney.model.SavingGoal;
import com.example.ismoney.model.User;
import com.example.ismoney.service.SavingGoalService;
import com.example.ismoney.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class savingGoalFormController {

    @FXML private TextField goalNameField;
    @FXML private TextField targetAmountField;
    @FXML private TextField currentAmountField;
    @FXML private DatePicker targetDatePicker;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Button simpanButton;
    @FXML private Button batalButton;

    private SavingGoalService savingGoalService;
    private UserDAOImpl userDAO;
    private Integer currentUserId;
    private SavingGoal currentGoal;

    @FXML
    public void initialize() {
        System.out.println("SavingGoalFormController initialized!");

        try {
            savingGoalService = new SavingGoalService();
            userDAO = new UserDAOImpl();

            currentUserId = getCurrentLoggedInUserId();
            System.out.println("Using user ID for saving goal form: " + currentUserId);

            setupFormDefaults();
            setupEventHandlers();

            System.out.println("SavingGoalFormController setup completed!");

        } catch (Exception e) {
            System.err.println("Error initializing SavingGoalFormController: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Kesalahan", "Gagal menginisialisasi form: " + e.getMessage());
        }
    }

    private Integer getCurrentLoggedInUserId() {
        try {
            Integer latestUserId = getLatestUserId();
            if (latestUserId != null) {
                System.out.println("Using latest user ID: " + latestUserId);
                return latestUserId;
            }

            Integer existingUserId = getFirstExistingUserId();
            if (existingUserId != null) {
                System.out.println("Using first existing user ID: " + existingUserId);
                return existingUserId;
            }

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

    private void setupFormDefaults() {
        statusComboBox.getItems().addAll("ACTIVE", "COMPLETED", "PAUSED");
        statusComboBox.setValue("ACTIVE");

        currentAmountField.setText("0");
        targetDatePicker.setValue(LocalDate.now().plusMonths(1));

        updateProgressDisplay();
    }

    private void setupEventHandlers() {
        // updae progress pas di update amount nya
        currentAmountField.textProperty().addListener((obs, oldVal, newVal) -> updateProgressDisplay());
        targetAmountField.textProperty().addListener((obs, oldVal, newVal) -> updateProgressDisplay());

        simpanButton.setOnAction(event -> handleSaveSavingGoal());

        batalButton.setOnAction(event -> handleBatal());
    }

    private void updateProgressDisplay() {
        try {
            String currentAmountText = currentAmountField.getText().trim();
            String targetAmountText = targetAmountField.getText().trim();

            if (currentAmountText.isEmpty() || targetAmountText.isEmpty()) {
                progressBar.setProgress(0);
                progressLabel.setText("0%");
                return;
            }

            BigDecimal currentAmount = new BigDecimal(currentAmountText);
            BigDecimal targetAmount = new BigDecimal(targetAmountText);

            if (targetAmount.compareTo(BigDecimal.ZERO) == 0) {
                progressBar.setProgress(0);
                progressLabel.setText("0%");
                return;
            }

            double progress = currentAmount.divide(targetAmount, 4, BigDecimal.ROUND_HALF_UP).doubleValue();
            progress = Math.min(progress, 1.0); // Cap at 100%

            progressBar.setProgress(progress);
            progressLabel.setText(String.format("%.1f%%", progress * 100));

        } catch (NumberFormatException e) {
            progressBar.setProgress(0);
            progressLabel.setText("0%");
        }
    }

    @FXML
    private void handleSaveSavingGoal() {
        System.out.println("Save saving goal button clicked!");

        if (currentUserId == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Tidak ada user yang valid. Silakan login terlebih dahulu.");
            return;
        }

        if (!validateInput()) return;

        try {
            // get data
            String goalName = goalNameField.getText().trim();
            BigDecimal targetAmount = new BigDecimal(targetAmountField.getText().trim());
            BigDecimal currentAmount = new BigDecimal(currentAmountField.getText().trim());
            LocalDate targetDate = targetDatePicker.getValue();
            String status = statusComboBox.getValue();

            SavingGoal savingGoal;
            if (currentGoal != null) {
                savingGoal = currentGoal;
                savingGoal.setGoalName(goalName);
                savingGoal.setTargetAmount(targetAmount);
                savingGoal.setCurrentAmount(currentAmount);
                savingGoal.setTargetDate(targetDate);
                savingGoal.setStatus(status);

                System.out.println("Attempting to update saving goal with user ID " + currentUserId + ": " + savingGoal.getGoalName());

                savingGoalService.updateSavingGoal(savingGoal, currentUserId);
                showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Saving goal berhasil diperbarui!");

            } else {
                // Create new goal
                savingGoal = new SavingGoal();
                savingGoal.setUserId(currentUserId);
                savingGoal.setGoalName(goalName);
                savingGoal.setTargetAmount(targetAmount);
                savingGoal.setCurrentAmount(currentAmount);
                savingGoal.setTargetDate(targetDate);
                savingGoal.setStatus(status);

                System.out.println("Attempting to create saving goal with user ID " + currentUserId + ": " + savingGoal.getGoalName());

                // Save via service
                savingGoalService.createSavingGoal(savingGoal, currentUserId);
                showAlert(Alert.AlertType.INFORMATION, "Berhasil", "Saving goal berhasil disimpan!");
            }

            clearForm();

            // Navigate to SavingGoalList after successful save
            Stage currentStage = (Stage) goalNameField.getScene().getWindow();
            SceneSwitcher.switchTo("savingGoals/savingGoalList.fxml", currentStage);
            System.out.println("Successfully navigated to SavingGoalList after save");

        } catch (SQLException e) {
            System.err.println("Database error saving saving goal: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal menyimpan saving goal ke database: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error saving saving goal: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Terjadi kesalahan saat menyimpan saving goal: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        // Validate goal name
        if (goalNameField.getText() == null || goalNameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Nama goal harus diisi!");
            goalNameField.requestFocus();
            return false;
        }

        // Validate target amount
        if (targetAmountField.getText() == null || targetAmountField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Target amount harus diisi!");
            targetAmountField.requestFocus();
            return false;
        }

        try {
            BigDecimal targetAmount = new BigDecimal(targetAmountField.getText().trim());
            if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Target amount harus lebih dari 0!");
                targetAmountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Target amount harus berupa angka yang valid!");
            targetAmountField.requestFocus();
            return false;
        }

        // Validate current amount
        if (currentAmountField.getText() == null || currentAmountField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Current amount harus diisi!");
            currentAmountField.requestFocus();
            return false;
        }

        try {
            BigDecimal currentAmount = new BigDecimal(currentAmountField.getText().trim());
            if (currentAmount.compareTo(BigDecimal.ZERO) < 0) {
                showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Current amount tidak boleh negatif!");
                currentAmountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Current amount harus berupa angka yang valid!");
            currentAmountField.requestFocus();
            return false;
        }

        // Validate target date
        if (targetDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Target date harus dipilih!");
            targetDatePicker.requestFocus();
            return false;
        }

        if (targetDatePicker.getValue().isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Target date tidak boleh di masa lalu!");
            targetDatePicker.requestFocus();
            return false;
        }

        // Validate status
        if (statusComboBox.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Validasi Gagal", "Status harus dipilih!");
            statusComboBox.requestFocus();
            return false;
        }

        return true;
    }

    private void handleBatal() {
        clearForm();
        SceneSwitcher.switchTo("savingGoals/savingGoalList.fxml", (Stage) batalButton.getScene().getWindow());
    }

    private void clearForm() {
        goalNameField.clear();
        targetAmountField.clear();
        currentAmountField.setText("0");
        targetDatePicker.setValue(LocalDate.now().plusMonths(1));
        statusComboBox.setValue("ACTIVE");
        currentGoal = null;
        updateProgressDisplay();
    }

    public void setGoalForEdit(SavingGoal goal) {
        this.currentGoal = goal;
        goalNameField.setText(goal.getGoalName());
        targetAmountField.setText(goal.getTargetAmount().toString());
        currentAmountField.setText(goal.getCurrentAmount().toString());
        targetDatePicker.setValue(goal.getTargetDate());
        statusComboBox.setValue(goal.getStatus());
        updateProgressDisplay();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}