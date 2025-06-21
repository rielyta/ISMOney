package com.example.ismoney.controller;

import com.example.ismoney.dao.UserDAO;
import com.example.ismoney.dao.UserDAOImpl;
import com.example.ismoney.model.User;
import com.example.ismoney.util.SceneSwitcher;
import com.example.ismoney.util.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
    private static final int LOCKOUT_DURATION_SECONDS = 30;

    private int loginAttempts = 0;
    private boolean isLockedOut = false;

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
    public void initialize() {
        UserSession.clearSession();

        System.clearProperty("current.user.id");

        loginAttempts = 0;
        isLockedOut = false;

        hideMessages();

        logger.info("Login page initialized, all sessions cleared");
    }

    @FXML
    protected void onLogin() {
        if (isLockedOut) {
            showError("Akun terkunci. Silakan tunggu beberapa saat sebelum mencoba lagi.");
            return;
        }

        hideMessages();
        setButtonState(false);

        try {
            String emailOrUsername = emailOrUsernameField.getText();
            String password = passwordField.getText();

            if (!validateInput(emailOrUsername, password)) {
                return;
            }

            User authenticatedUser = userDAO.authenticateUser(emailOrUsername.trim(), password);

            if (authenticatedUser == null) {
                handleFailedLogin();
                return;
            }

            handleSuccessfulLogin(authenticatedUser);

        } catch (Exception e) {
            logger.severe("Error during login: " + e.getMessage());
            showError("Terjadi kesalahan sistem. Silakan coba lagi.");
            e.printStackTrace();
        } finally {
            if (!isLockedOut) {
                setButtonState(true);
            }
        }
    }

    private boolean validateInput(String emailOrUsername, String password) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            showError("Email/Username wajib diisi.");
            loginAttempts++;
            setButtonState(true);
            return false;
        }

        if (password == null || password.isEmpty()) {
            showError("Password wajib diisi.");
            loginAttempts++;
            setButtonState(true);
            return false;
        }

        if (emailOrUsername.length() > 255 || password.length() > 255) {
            showError("Input terlalu panjang.");
            loginAttempts++;
            setButtonState(true);
            return false;
        }

        return true;
    }

    private void handleFailedLogin() {
        loginAttempts++;

        String errorMsg = "Email/Username atau password salah. " +
                "Percobaan: " + loginAttempts + "/" + MAX_LOGIN_ATTEMPTS;

        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            initiateAccountLockout();
            errorMsg = "Terlalu banyak percobaan login gagal. Akun dikunci selama " +
                    LOCKOUT_DURATION_SECONDS + " detik.";
        }

        showError(errorMsg);

        if (!isLockedOut) {
            setButtonState(true);
        }

        logger.warning("Failed login attempt " + loginAttempts + " for user: " +
                emailOrUsernameField.getText());
    }

    private void handleSuccessfulLogin(User authenticatedUser) {
        try {
            userDAO.updateLastLogin(authenticatedUser.getId());

            UserSession.setUserSession(authenticatedUser.getId(), authenticatedUser.getUsername());

            System.setProperty("current.user.id", String.valueOf(authenticatedUser.getId()));

            loginAttempts = 0;
            isLockedOut = false;

            showSuccess("Login berhasil! Selamat datang, " + authenticatedUser.getUsername() + "!");

            passwordField.clear();

            logger.info("User logged in successfully: " + authenticatedUser.getUsername() +
                    " (ID: " + authenticatedUser.getId() + ")");

            navigateToDashboard();

        } catch (Exception e) {
            logger.severe("Error during successful login handling: " + e.getMessage());
            showError("Login berhasil tetapi terjadi kesalahan. Silakan coba lagi.");
            e.printStackTrace();
            setButtonState(true);
        }
    }

    private void navigateToDashboard() {
        Task<Void> navigationTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(1000);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    try {
                        if (!UserSession.isSessionValid()) {
                            showError("Session tidak valid. Silakan login kembali.");
                            return;
                        }

                        logger.info("Navigating to dashboard for user ID: " + UserSession.getCurrentUserId());
                        SceneSwitcher.switchTo("Dashboard.fxml", (Stage) loginButton.getScene().getWindow());

                    } catch (Exception e) {
                        logger.severe("Error switching to dashboard: " + e.getMessage());
                        showError("Gagal mengalihkan ke dashboard: " + e.getMessage());
                        setButtonState(true);
                        e.printStackTrace();
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.severe("Navigation task failed: " + getException().getMessage());
                    showError("Gagal mengalihkan ke dashboard.");
                    setButtonState(true);
                });
            }
        };

        Thread navigationThread = new Thread(navigationTask);
        navigationThread.setDaemon(true);
        navigationThread.start();
    }

    private void initiateAccountLockout() {
        isLockedOut = true;
        setButtonState(false);

        Task<Void> lockoutTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(LOCKOUT_DURATION_SECONDS * 1000);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    loginAttempts = 0;
                    isLockedOut = false;
                    setButtonState(true);
                    hideMessages();
                    logger.info("Account lockout period ended");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.severe("Lockout task failed: " + getException().getMessage());
                    loginAttempts = 0;
                    isLockedOut = false;
                    setButtonState(true);
                });
            }
        };

        Thread lockoutThread = new Thread(lockoutTask);
        lockoutThread.setDaemon(true);
        lockoutThread.start();

        logger.warning("Account locked out for " + LOCKOUT_DURATION_SECONDS + " seconds");
    }

    @FXML
    private void onSignupClick() {
        try {
            UserSession.clearSession();
            System.clearProperty("current.user.id");

            SceneSwitcher.switchTo("Register.fxml", (Stage) signupLabel.getScene().getWindow());

        } catch (Exception e) {
            logger.severe("Error navigating to signup: " + e.getMessage());
            showError("Gagal mengalihkan ke halaman pendaftaran.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorMessage.setText(message);
            errorMessage.setVisible(true);
            successMessage.setVisible(false);
        });
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            successMessage.setText(message);
            successMessage.setVisible(true);
            errorMessage.setVisible(false);
        });
    }

    private void hideMessages() {
        Platform.runLater(() -> {
            errorMessage.setVisible(false);
            successMessage.setVisible(false);
        });
    }

    private void setButtonState(boolean enabled) {
        Platform.runLater(() -> {
            loginButton.setDisable(!enabled);
        });
    }
}