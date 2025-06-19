package com.example.ismoney;

import com.example.ismoney.dao.UserDAO;
import com.example.ismoney.dao.UserDAOImpl;
import com.example.ismoney.model.user.User;
import com.example.ismoney.util.SceneSwitcher;
import com.example.ismoney.util.ValidationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.logging.Logger;

public class HelloController {
    private static final Logger logger = Logger.getLogger(HelloController.class.getName());

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorMessage;
    @FXML private Label successMessage;
    @FXML private Label loginLabel;
    @FXML private Button signupButton;

    private final UserDAO userDAO = new UserDAOImpl();

    @FXML
    protected void onSignup() {
        hideMessages();
        setButtonState(false);

        try {
            String username = usernameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            String validationError = validateRegistrationInput(username, email, password, confirmPassword);
            if (validationError != null) {
                showError(validationError);
                return;
            }

            if (userDAO.isEmailExists(email)) {
                showError("Email sudah digunakan.");
                return;
            }

            if (userDAO.isUsernameExists(username)) {
                showError("Username sudah digunakan.");
                return;
            }

            String hashedPassword = userDAO.hashPassword(password);
            User newUser = new User(username, email, hashedPassword);

            boolean saved = userDAO.save(newUser);
            if (!saved) {
                showError("Gagal menyimpan data pengguna. Silakan coba lagi.");
                return;
            }

            showSuccess("Registrasi berhasil! Mengalihkan ke dashboard...");
            clearForm();

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(1500);
                    return null;
                }

                @Override
                protected void succeeded() {
                    try {
                        SceneSwitcher.switchTo("Dashboard.fxml", (Stage) signupButton.getScene().getWindow());
                    } catch (Exception e) {
                        logger.severe("Error switching to dashboard: " + e.getMessage());
                        showError("Gagal mengalihkan ke dashboard.");
                    }
                }
            };

            new Thread(task).start();

        } catch (Exception e) {
            logger.severe("Error during registration: " + e.getMessage());
            showError("Terjadi kesalahan sistem. Silakan coba lagi.");
        } finally {
            setButtonState(true);
        }
    }

    @FXML
    private void onLoginClick() {
        try {
            SceneSwitcher.switchTo("Login.fxml", (Stage) loginLabel.getScene().getWindow());
        } catch (Exception e) {
            logger.severe("Error switching to login: " + e.getMessage());
            showError("Gagal mengalihkan ke halaman login.");
        }
    }

    private String validateRegistrationInput(String username, String email, String password, String confirmPassword) {
        if (username == null || username.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.isEmpty() ||
                confirmPassword == null || confirmPassword.isEmpty()) {
            return "Semua field wajib diisi.";
        }

        if (!ValidationUtils.isValidEmail(email)) {
            return "Format email tidak valid.";
        }

        if (!ValidationUtils.isValidUsername(username)) {
            return "Username harus 3-20 karakter dan hanya boleh mengandung huruf, angka, dan underscore.";
        }

        if (!ValidationUtils.isStrongPassword(password)) {
            return "Password harus minimal 8 karakter dengan kombinasi huruf besar, huruf kecil, dan angka.";
        }

        if (!password.equals(confirmPassword)) {
            return "Password tidak cocok.";
        }

        return null;
    }

    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
        successMessage.setVisible(false);
    }

    private void showSuccess(String message) {
        successMessage.setText(message);
        successMessage.setVisible(true);
        errorMessage.setVisible(false);
    }

    private void hideMessages() {
        errorMessage.setVisible(false);
        successMessage.setVisible(false);
    }

    private void setButtonState(boolean enabled) {
        signupButton.setDisable(!enabled);
    }

    private void clearForm() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
}