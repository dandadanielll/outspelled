package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Main menu (wizard theme). LAN-only: Start Game → connect; Options, Shop, Tutorial, Quit.
 */
public class MenuController {

    @FXML private Button startGameButton;
    @FXML private Button optionsButton;
    @FXML private Button shopButton;
    @FXML private Button tutorialButton;
    @FXML private Button quitButton;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> windowModeCombo;

    @FXML
    public void initialize() {
        if (windowModeCombo != null) {
            windowModeCombo.setItems(FXCollections.observableArrayList(
                    "Windowed", "Windowed Fullscreen", "Fullscreen"));
            switch (Main.getWindowMode()) {
                case WINDOWED: windowModeCombo.getSelectionModel().select(0); break;
                case WINDOWED_FULLSCREEN: windowModeCombo.getSelectionModel().select(1); break;
                case FULLSCREEN: windowModeCombo.getSelectionModel().select(2); break;
            }
            windowModeCombo.setOnAction(e -> {
                int idx = windowModeCombo.getSelectionModel().getSelectedIndex();
                if (idx == 0) Main.setWindowMode(Main.WindowMode.WINDOWED);
                else if (idx == 1) Main.setWindowMode(Main.WindowMode.WINDOWED_FULLSCREEN);
                else if (idx == 2) Main.setWindowMode(Main.WindowMode.FULLSCREEN);
            });
        }
    }

    @FXML
    private void onStartGameClicked() {
        Main.navigateTo("connect-view.fxml");
    }

    @FXML
    private void onOptionsClicked() {
        // TODO: options/settings screen
        if (errorLabel != null) errorLabel.setText("Options (placeholder)");
    }

    @FXML
    private void onShopClicked() {
        // Placeholder: no in-game currency yet
        if (errorLabel != null) errorLabel.setText("Shop coming soon.");
    }

    @FXML
    private void onTutorialClicked() {
        // Placeholder
        if (errorLabel != null) errorLabel.setText("Tutorial coming soon.");
    }

    @FXML
    private void onQuitClicked() {
        Stage stage = Main.getPrimaryStage();
        if (stage != null) stage.close();
        Platform.exit();
    }
}
