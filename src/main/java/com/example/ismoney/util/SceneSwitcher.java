package com.example.ismoney.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneSwitcher {
    public static void switchTo(String fxmlFileName, Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/view/" + fxmlFileName));
            Parent root = loader.load();

            Scene newScene = new Scene(root, 756, 491);
            currentStage.setScene(newScene);
            currentStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
