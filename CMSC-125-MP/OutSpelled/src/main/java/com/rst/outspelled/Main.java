package com.rst.outspelled;

import com.rst.outspelled.util.SoundManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    //Window states the game can be in using the enum keyword
    public enum WindowMode { WINDOWED, WINDOWED_FULLSCREEN, FULLSCREEN }

    //Sets the startup screen (default is windowed at 900x650 res)
    private static Stage primaryStage; //Static so stage shared to any object instance
    private static WindowMode currentMode = WindowMode.FULLSCREEN;
    private static final int WINDOWED_WIDTH = 900;
    private static final int WINDOWED_HEIGHT = 650;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage; //stores the javafx created stage into static variable to be used by other classes
        stage.setTitle("OutSpelled");
        stage.setResizable(true);

        // Hide the ugly "Press ESC to exit full-screen mode" overlay
        stage.setFullScreenExitHint("");

        //the window's size cant be smaller than the default size
        stage.setMinWidth(WINDOWED_WIDTH);
        stage.setMinHeight(WINDOWED_HEIGHT);

        //preload all SFX clips before any screen is shown
        SoundManager.initialize();
        javafx.scene.text.Font.loadFont(Main.class.getResourceAsStream("/assets/fonts/PixelifySans-VariableFont_wght.ttf"), 14);

        //calls method to open landing page of the app, the profile selection screen
        navigateTo("landing-view.fxml");

        //calls method that applies the set window mode state
        applyWindowMode();

        //window is shown
        stage.show();
    }

    //method for other controllers to change window size when necessary
    public static void setWindowMode(WindowMode mode) {
        currentMode = mode; //stores the current window mode (fullscreen, windowFull, or full)
        if (primaryStage != null) applyWindowMode(); //ensures a window exists before some other class decides to use this method
    }

    //self explanatory
    public static WindowMode getWindowMode() {
        return currentMode;
    }

    //
    private static void applyWindowMode() {
        if (primaryStage == null) return;

        switch (currentMode) {
            case WINDOWED:
                if (primaryStage.isFullScreen()) primaryStage.setFullScreen(false);
                if (primaryStage.isMaximized()) primaryStage.setMaximized(false);
                primaryStage.setWidth(WINDOWED_WIDTH);
                primaryStage.setHeight(WINDOWED_HEIGHT);
                primaryStage.centerOnScreen();
                break;
            case WINDOWED_FULLSCREEN:
                if (primaryStage.isFullScreen()) primaryStage.setFullScreen(false);
                if (!primaryStage.isMaximized()) primaryStage.setMaximized(true);
                break;
            case FULLSCREEN:
                if (primaryStage.isMaximized()) primaryStage.setMaximized(false);
                if (!primaryStage.isFullScreen()) primaryStage.setFullScreen(true);
                break;
        }
    }

    //the scene router, controllers use this to switch between screens stored in the db
    public static void navigateTo(String fxmlFile) {
        try {
            //If the filename already starts with / → use it as-is
            //If not → prepend the full package path
            String path = fxmlFile.startsWith("/") ? fxmlFile : "/com/rst/outspelled/" + fxmlFile; //ternary operator (shortened if/else statement)

            //passes the path of the file and returns a url to that file and stores it in a variable called location
            java.net.URL location = Main.class.getResource(path);

            //If getResource() returned null, the file doesn't exist (wrong name, wrong folder) and throws an exception
            if (location == null) {
                throw new IOException("FXML resource not found: " + path);
            }

            FXMLLoader loader = new FXMLLoader(location); //create fxmlLoader object
            javafx.scene.Parent root = loader.load();
            
            if (primaryStage.getScene() == null) {
                primaryStage.setScene(new Scene(root)); //create the fxml scene only the first time
                applyWindowMode(); // apply window mode once
            } else {
                primaryStage.getScene().setRoot(root); //swap the root node to avoid window resize/flicker bugs
            }
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile + ": " + e.getMessage()); //formatted error code pag wala yung file
        }
    }

    // navigates to the next screen with a slide-up transition for both the current and new screen
    public static void navigateWithSlideUpTransition(String fxmlFile) {
        try {
            String path = fxmlFile.startsWith("/") ? fxmlFile : "/com/rst/outspelled/" + fxmlFile;
            java.net.URL location = Main.class.getResource(path);

            if (location == null) {
                throw new IOException("FXML resource not found: " + path);
            }

            FXMLLoader loader = new FXMLLoader(location);
            javafx.scene.Parent newRoot = loader.load();

            Scene currentScene = primaryStage.getScene();
            if (currentScene != null && currentScene.getRoot() != null) {
                javafx.scene.Parent currentRoot = currentScene.getRoot();
                
                // Temporary container to hold both scenes during the transition
                javafx.scene.layout.StackPane transitionContainer = new javafx.scene.layout.StackPane();
                transitionContainer.getChildren().addAll(currentRoot, newRoot);
                
                double sceneHeight = currentScene.getHeight() > 0 ? currentScene.getHeight() : WINDOWED_HEIGHT;
                
                // Start the new scene exactly at the bottom of the current screen
                newRoot.setTranslateY(sceneHeight);
                
                // Replace the scene's root with our transition container
                currentScene.setRoot(transitionContainer);
                
                // Animate old screen moving up
                javafx.animation.TranslateTransition slideOut = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(1500), currentRoot);
                slideOut.setByY(-sceneHeight);
                slideOut.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                
                // Animate new screen moving up
                javafx.animation.TranslateTransition slideIn = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(1500), newRoot);
                slideIn.setByY(-sceneHeight);
                slideIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                
                slideIn.setOnFinished(e -> {
                    // Reset properties and lock in the new scene
                    newRoot.setTranslateY(0);
                    currentScene.setRoot(newRoot);
                });
                
                slideOut.play();
                slideIn.play();
            } else {
                Scene scene = new Scene(newRoot);
                primaryStage.setScene(scene);
            }
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile + ": " + e.getMessage());
        }
    }

    //method for other controllers to call the window
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    //Called by the launcher to launch the application
    public static void main(String[] args) {
        launch();
    }
}