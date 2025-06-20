package com.example.ismoney.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.logging.Level;

public class SceneSwitcher {
    private static final Logger logger = Logger.getLogger(SceneSwitcher.class.getName());

    public static void switchTo(String fxmlFileName, Stage stage) {
        try {
            logger.info("Attempting to switch to: " + fxmlFileName);

            // Try different resource paths
            URL fxmlUrl = null;

            // Try path 1: /com/example/ismoney/
            fxmlUrl = SceneSwitcher.class.getResource("/com/example/ismoney/" + fxmlFileName);
            if (fxmlUrl == null) {
                logger.warning("FXML not found at: /com/example/ismoney/" + fxmlFileName);

                // Try path 2: root of resources
                fxmlUrl = SceneSwitcher.class.getResource("/" + fxmlFileName);
                if (fxmlUrl == null) {
                    logger.warning("FXML not found at: /" + fxmlFileName);

                    // Try path 3: relative to current class
                    fxmlUrl = SceneSwitcher.class.getResource(fxmlFileName);
                    if (fxmlUrl == null) {
                        throw new IOException("FXML file not found: " + fxmlFileName);
                    }
                }
            }

            logger.info("Found FXML at: " + fxmlUrl.toString());

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();

            logger.info("Successfully switched to scene: " + fxmlFileName);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading FXML file: " + fxmlFileName, e);

            // Print more debugging info
            logger.severe("Current working directory: " + System.getProperty("user.dir"));
            logger.severe("Classpath: " + System.getProperty("java.class.path"));

            throw new RuntimeException("Failed to load scene: " + fxmlFileName + " - " + e.getMessage(), e);
        }
    }

    public static <T> T switchToAndGetController(String fxmlFileName, Stage stage) {
        try {
            logger.info("Attempting to switch to and get controller: " + fxmlFileName);

            URL fxmlUrl = null;

            // Try different resource paths
            fxmlUrl = SceneSwitcher.class.getResource("/com/example/ismoney/" + fxmlFileName);
            if (fxmlUrl == null) {
                fxmlUrl = SceneSwitcher.class.getResource("/" + fxmlFileName);
                if (fxmlUrl == null) {
                    fxmlUrl = SceneSwitcher.class.getResource(fxmlFileName);
                    if (fxmlUrl == null) {
                        throw new IOException("FXML file not found: " + fxmlFileName);
                    }
                }
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.centerOnScreen();

            logger.info("Successfully switched to scene: " + fxmlFileName);

            return loader.getController();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading FXML file: " + fxmlFileName, e);
            throw new RuntimeException("Failed to load scene: " + fxmlFileName + " - " + e.getMessage(), e);
        }
    }
}