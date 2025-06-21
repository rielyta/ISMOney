package com.example.ismoney.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SceneSwitcher {
    private static final Logger logger = Logger.getLogger(SceneSwitcher.class.getName());

    public static void switchTo(String fxmlFileName, Stage stage) {
        try {
            logger.info("Akan pergi ke: " + fxmlFileName);

            URL fxmlUrl = null;

            fxmlUrl = SceneSwitcher.class.getResource("/com/example/ismoney/" + fxmlFileName);
            if (fxmlUrl == null) {
                logger.warning("FXML tidak ditemukan di: /com/example/ismoney/" + fxmlFileName);

                fxmlUrl = SceneSwitcher.class.getResource("/" + fxmlFileName);
                if (fxmlUrl == null) {
                    logger.warning("FXML tidak ditemukan di: /" + fxmlFileName);

                    fxmlUrl = SceneSwitcher.class.getResource(fxmlFileName);
                    if (fxmlUrl == null) {
                        throw new IOException("FXML tidak ditemukan: " + fxmlFileName);
                    }
                }
            }

            logger.info("FXML ditemukan di: " + fxmlUrl.toString());

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();

            logger.info("Berhasil beralih ke scene: " + fxmlFileName);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Gagal memuat FXML: " + fxmlFileName, e);

            logger.severe("Direktori kerja saat ini: " + System.getProperty("user.dir"));
            logger.severe("Classpath: " + System.getProperty("java.class.path"));

            throw new RuntimeException("Gagal memuat scene: " + fxmlFileName + " - " + e.getMessage(), e);
        }
    }

    private static void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void openModal(String fxmlFileName, String title) {
        try {
            logger.info("Membuka modal: " + fxmlFileName);

            URL fxmlUrl = null;

            fxmlUrl = SceneSwitcher.class.getResource("/com/example/ismoney/" + fxmlFileName);
            if (fxmlUrl == null) {
                logger.warning("FXML tidak ditemukan di: /com/example/ismoney/" + fxmlFileName);

                fxmlUrl = SceneSwitcher.class.getResource("/" + fxmlFileName);
                if (fxmlUrl == null) {
                    logger.warning("FXML tidak ditemukan di: /" + fxmlFileName);

                    fxmlUrl = SceneSwitcher.class.getResource(fxmlFileName);
                    if (fxmlUrl == null) {
                        throw new IOException("FXML tidak ditemukan: " + fxmlFileName);
                    }
                }
            }

            logger.info("FXML ditemukan di: " + fxmlUrl.toString());

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.setTitle(title);
            modalStage.setScene(new Scene(root));
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.centerOnScreen();

            modalStage.showAndWait();

            logger.info("Berhasil membuka modal: " + fxmlFileName);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Gagal memuat modal FXML: " + fxmlFileName, e);
            throw new RuntimeException("Gagal memuat modal: " + fxmlFileName + " - " + e.getMessage(), e);
        }
    }


    public static boolean logout(Stage currentStage, String loginFxmlFileName) {
        logger.info("Attempting logout process");

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Keluar");
        confirmAlert.setHeaderText("Apakah Anda yakin ingin keluar?");

        ButtonType yesButton = new ButtonType("Ya");
        ButtonType noButton = new ButtonType("Batal");
        confirmAlert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == yesButton) {
            try {
                logger.info("Pengguna setuju untuk keluar, maka akan beralih ke halaman login");

                switchTo(loginFxmlFileName, currentStage);

                currentStage.setTitle("Register - isMoney");

                showAlert("Berhasil Keluar", "Anda telah berhasil keluar dari aplikasi.", Alert.AlertType.INFORMATION);

                logger.info("Logout completed successfully");
                return true;

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error during logout process", e);
                showAlert("Kesalahan", "Gagal melakukan logout: " + e.getMessage(), Alert.AlertType.ERROR);
                return false;
            }
        } else {
            logger.info("Pengguna gagal untuk keluar");
            return false;
        }
    }
}