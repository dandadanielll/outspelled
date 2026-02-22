package com.rst.outspelled;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public enum WindowMode { WINDOWED, WINDOWED_FULLSCREEN, FULLSCREEN }

    private static Stage primaryStage;
    private static WindowMode currentMode = WindowMode.WINDOWED;
    private static final int WINDOWED_WIDTH = 900;
    private static final int WINDOWED_HEIGHT = 650;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("OutSpelled");
        stage.setResizable(true);
        stage.setMinWidth(WINDOWED_WIDTH);
        stage.setMinHeight(WINDOWED_HEIGHT);
        navigateTo("login-view.fxml");
        applyWindowMode();
        stage.show();
    }

    public static void setWindowMode(WindowMode mode) {
        currentMode = mode;
        if (primaryStage != null) applyWindowMode();
    }

    public static WindowMode getWindowMode() { return currentMode; }

    private static void applyWindowMode() {
        if (primaryStage == null) return;
        primaryStage.setFullScreen(false);
        primaryStage.setMaximized(false);
        switch (currentMode) {
            case WINDOWED:
                primaryStage.setWidth(WINDOWED_WIDTH);
                primaryStage.setHeight(WINDOWED_HEIGHT);
                primaryStage.centerOnScreen();
                break;
            case WINDOWED_FULLSCREEN:
                primaryStage.setMaximized(true);
                break;
            case FULLSCREEN:
                primaryStage.setFullScreen(true);
                break;
        }
    }

    public static void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource(fxmlFile)
            );
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile + ": " + e.getMessage());
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) {
        launch();
    }
}