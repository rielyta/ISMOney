package com.example.ismoney;

import com.example.ismoney.model.user.User;
import com.example.ismoney.util.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private Button loginBtn;
    @FXML private Label errorMessage;
    @FXML private Label successMessage;

    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty()) {
            showError("Email tidak boleh kosong.");
            return;
        }
        if (password.isEmpty()) {
            showError("Kata sandi tidak boleh kosong.");
            return;
        }

        User user = new User(email, password);
        boolean success = user.checkCredentials();

        if (success) {
            showSuccess("Login berhasil!");
            // Setelah login, dapat dialihkan ke Dashboard
            SceneSwitcher.switchTo("Dashboard.fxml", (Stage) emailField.getScene().getWindow());
        } else {
            showError("Email atau kata sandi salah.");
        }
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
}
