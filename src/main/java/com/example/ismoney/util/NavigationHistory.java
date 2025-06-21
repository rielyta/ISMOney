package com.example.ismoney.util;

import javafx.stage.Stage;
import java.util.Stack;
import java.util.logging.Logger;

public class NavigationHistory {
    private static final Stack<String> sceneHistory = new Stack<>();
    private static Stage primaryStage;
    private static final Logger logger = Logger.getLogger(NavigationHistory.class.getName());

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
        logger.info("NavigationHistory: Primary stage set");
    }

    // Tambah scene ke history
    public static void addToHistory(String sceneName) {
        // Hindari duplikasi scene yang sama berturut-turut
        if (sceneHistory.isEmpty() || !sceneHistory.peek().equals(sceneName)) {
            sceneHistory.push(sceneName);
            logger.info("Added to history: " + sceneName + " (Total: " + sceneHistory.size() + ")");
        } else {
            logger.info("Skipped duplicate scene: " + sceneName);
        }
    }

    // Cek apakah ada scene sebelumnya
    public static boolean hasPreviousScene() {
        return sceneHistory.size() > 1;
    }

    // Kembali ke scene sebelumnya dan return nama scene
    public static String goBack() {
        if (hasPreviousScene()) {
            // Hapus scene saat ini
            String currentScene = sceneHistory.pop();
            // Ambil scene sebelumnya
            String previousScene = sceneHistory.peek();

            logger.info("Going back from " + currentScene + " to " + previousScene);
            return previousScene;
        } else {
            logger.warning("No previous scene to go back to");
            return null;
        }
    }

    // Reset history (misal saat logout)
    public static void clearHistory() {
        int size = sceneHistory.size();
        sceneHistory.clear();
        logger.info("Navigation history cleared (was " + size + " items)");
    }

    // Get current scene
    public static String getCurrentScene() {
        return sceneHistory.isEmpty() ? null : sceneHistory.peek();
    }

    // Get history size
    public static int getHistorySize() {
        return sceneHistory.size();
    }

    // Get all history (for debugging)
    public static String getHistoryString() {
        return sceneHistory.toString();
    }

    // Check if specific scene is in history
    public static boolean containsScene(String sceneName) {
        return sceneHistory.contains(sceneName);
    }

    // Remove specific scene from history (utility method)
    public static void removeFromHistory(String sceneName) {
        if (sceneHistory.remove(sceneName)) {
            logger.info("Removed scene from history: " + sceneName);
        }
    }

    // Peek at previous scene without removing it
    public static String peekPreviousScene() {
        if (sceneHistory.size() > 1) {
            String current = sceneHistory.pop();
            String previous = sceneHistory.peek();
            sceneHistory.push(current); // Put it back
            return previous;
        }
        return null;
    }
}