package com.example.ismoney.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SceneSwitcher {

    private static final Logger logger = Logger.getLogger(SceneSwitcher.class.getName());
    public static void switchTo(String fxmlFileName, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(SceneSwitcher.class.getResource("/com/example/ismoney/" + fxmlFileName));

            Parent root = loader.load();

            Scene scene = new Scene(root);

            stage.setScene(scene);

            stage.centerOnScreen();

            logger.info("Successfully switched to scene: " + fxmlFileName);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading FXML file: " + fxmlFileName, e);
            throw new RuntimeException("Failed to load scene: " + fxmlFileName, e);
        }
    }
    public static <T> T switchToAndGetController(String fxmlFileName, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(SceneSwitcher.class.getResource("/com/example/ismoney/" + fxmlFileName));

            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();

            logger.info("Successfully switched to scene: " + fxmlFileName);

            return loader.getController();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading FXML file: " + fxmlFileName, e);
            throw new RuntimeException("Failed to load scene: " + fxmlFileName, e);
        }
    }
}