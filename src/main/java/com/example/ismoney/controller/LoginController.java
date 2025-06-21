package com.example.ismoney.controller;

import com.example.ismoney.dao.UserDAO;
import com.example.ismoney.dao.UserDAOImpl;
import com.example.ismoney.model.User;
import com.example.ismoney.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class LoginController {
    private static final Logger logger = Logger.getLogger(LoginController.class.getName());
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private int loginAttempts = 0;

    @FXML
    private TextField emailOrUsernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorMessage;
    @FXML
    private Label successMessage;
    @FXML
    private Label signupLabel;
    @FXML
    private Button loginButton;

    private final UserDAO userDAO = new UserDAOImpl();

    @FXML
    protected void onLogin() {
        hideMessages();
        setButtonState(false);

        try {
            if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                showError("Terlalu banyak percobaan login. Silakan tunggu beberapa saat.");
                return;
            }

            String emailOrUsername = emailOrUsernameField.getText();
            String password = passwordField.getText();

            if (emailOrUsername == null || emailOrUsername.trim().isEmpty() ||
                    password == null || password.isEmpty()) {
                showError("Email/Username dan password wajib diisi.");
                loginAttempts++;
                return;
            }

            User authenticatedUser = userDAO.authenticateUser(emailOrUsername.trim(), password);

            if (authenticatedUser == null) {
                loginAttempts++;
                showError("Email/Username atau password salah. Percobaan: " + loginAttempts + "/" + MAX_LOGIN_ATTEMPTS);

                if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                    setButtonState(false);
                    javafx.concurrent.Task<Void> resetTask = new javafx.concurrent.Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            Thread.sleep(30000);
                            return null;
                        }

                        @Override
                        protected void succeeded() {
                            loginAttempts = 0;
                            setButtonState(true);
                        }
                    };
                    new Thread(resetTask).start();
                }
                return;
            }

            userDAO.updateLastLogin(authenticatedUser.getId());
            System.setProperty("current.user.id", String.valueOf(authenticatedUser.getId()));
            showSuccess("Login berhasil!");

            loginAttempts = 0;

            passwordField.clear();

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(1000);
                    return null;
                }

                @Override
                protected void succeeded() {
                    try {
                        SceneSwitcher.switchTo("Dashboard.fxml", (Stage) loginButton.getScene().getWindow());
                    } catch (Exception e) {
                        logger.severe("Error switching to dashboard: " + e.getMessage());
                        showError("Gagal mengalihkan ke dashboard: " + e.getMessage());
                        setButtonState(true);
                    }
                }
            };

            new Thread(task).start();

        } catch (Exception e) {
            logger.severe("Error during login: " + e.getMessage());
            showError("Terjadi kesalahan sistem. Silakan coba lagi.");
        } finally {
            if (loginAttempts < MAX_LOGIN_ATTEMPTS) {
                setButtonState(true);
            }
        }
    }

    @FXML
    private void onSignupClick() {
        SceneSwitcher.switchTo("Register.fxml", (Stage) signupLabel.getScene().getWindow());
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
        loginButton.setDisable(!enabled);
    }
}