package com.example.ismoney.service;

import com.example.ismoney.dao.SavingGoalDAO;
import com.example.ismoney.model.SavingGoal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class SavingGoalService {
    private final SavingGoalDAO savingGoalDAO;

    public SavingGoalService() {
        this.savingGoalDAO = new SavingGoalDAO();
    }

    // CREATE - Membuat goal baru untuk user tertentu
    public void createSavingGoal(SavingGoal savingGoal, Integer userId) throws SQLException {
        savingGoal.setUserId(userId);
        validateSavingGoal(savingGoal);
        savingGoalDAO.addSavingGoal(savingGoal);
    }

    // READ - Mengambil goal aktif berdasarkan user
    public List<SavingGoal> getActiveSavingGoalsByUserId(Integer userId) throws SQLException {
        return savingGoalDAO.getActiveSavingGoalsByUserId(userId);
    }

    // UPDATE - Memperbarui goal
    public void updateSavingGoal(SavingGoal savingGoal, Integer userId) throws SQLException {
        savingGoal.setUserId(userId);
        validateSavingGoal(savingGoal);
        savingGoalDAO.updateSavingGoal(savingGoal);
    }

    // VALIDATION - Validasi input goal
    private void validateSavingGoal(SavingGoal savingGoal) {
        if (savingGoal.getUserId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        if (savingGoal.getGoalName() == null || savingGoal.getGoalName().trim().isEmpty()) {
            throw new IllegalArgumentException("Saving goal name cannot be empty");
        }

        if (savingGoal.getTargetAmount() == null || savingGoal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target amount must be positive");
        }

        if (savingGoal.getCurrentAmount() == null || savingGoal.getCurrentAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current amount cannot be negative");
        }

        if (savingGoal.getTargetDate() == null || savingGoal.getTargetDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Target date must be today or in the future");
        }
    }

    @Deprecated
    public void createSavingGoal(SavingGoal savingGoal) throws SQLException {
        System.err.println("Warning: createSavingGoal(SavingGoal) is deprecated. Use createSavingGoal(SavingGoal, Integer) instead.");
        throw new IllegalArgumentException("User ID is required. Use createSavingGoal(SavingGoal, Integer userId) instead.");
    }

    @Deprecated
    public List<SavingGoal> getAllSavingGoals() throws SQLException {
        System.err.println("Warning: getAllSavingGoals() is deprecated. Use getSavingGoalsByUserId(Integer) instead.");
        throw new IllegalArgumentException("User ID is required. Use getSavingGoalsByUserId(Integer userId) instead.");
    }

    @Deprecated
    public List<SavingGoal> getActiveSavingGoals() throws SQLException {
        System.err.println("Warning: getActiveSavingGoals() is deprecated. Use getActiveSavingGoalsByUserId(Integer) instead.");
        throw new IllegalArgumentException("User ID is required. Use getActiveSavingGoalsByUserId(Integer userId) instead.");
    }

    @Deprecated
    public SavingGoal getSavingGoalById(int id) throws SQLException {
        System.err.println("Warning: getSavingGoalById(int) is deprecated. Use getSavingGoalById(int, Integer) instead.");
        throw new IllegalArgumentException("User ID is required. Use getSavingGoalById(int goalId, Integer userId) instead.");
    }
}