package com.rst.outspelled;

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
    private static WindowMode currentMode = WindowMode.WINDOWED;
    private static final int WINDOWED_WIDTH = 900;
    private static final int WINDOWED_HEIGHT = 650;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage; //stores the javafx created stage into static variable to be used by other classes
        stage.setTitle("OutSpelled");
        stage.setResizable(true);

        //the window's size cant be smaller than the default size
        stage.setMinWidth(WINDOWED_WIDTH);
        stage.setMinHeight(WINDOWED_HEIGHT);

        //calls method to open landing page of the app, the profile selection screen
        navigateTo("profile-view.fxml");

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
        //returns nothing if no window exists
        if (primaryStage == null) return;

        //clear the window state first before applying any new state
        primaryStage.setFullScreen(false);
        primaryStage.setMaximized(false);

        //checks which of the 3 modes is currently set and handles each one differently
        switch (currentMode) {
            //Sets the window to exactly 900×650 and moves it to the center of the screen
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
            Scene scene = new Scene(loader.load()); //create the fxml scene
            primaryStage.setScene(scene); //sets the window to the fxml scene created
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlFile + ": " + e.getMessage()); //formatted error code pag wala yung file
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