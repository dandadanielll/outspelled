package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.ai.AiOpponent;
import com.rst.outspelled.ai.StandardAi;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.util.SoundManager;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MenuController {

    @FXML
    private VBox primaryButtonsBox;
    @FXML
    private VBox secondaryButtonsBox;
    @FXML
    private StackPane quitButtonContainer;
    @FXML
    private Label errorLabel;
    @FXML
    private ComboBox<String> windowModeCombo;

    @FXML
    public void initialize() {
        SoundManager.startBgm("MenuBGM.mp3");

        StackPane startBtn = buildPrimaryButton("⚡   Start Game", false);
        StackPane aiBtn = buildPrimaryButton("🤖   Play vs AI", true);
        startBtn.setOnMouseClicked(e -> onStartGameClicked());
        aiBtn.setOnMouseClicked(e -> onPlayVsAiClicked());
        primaryButtonsBox.getChildren().addAll(startBtn, aiBtn);

        StackPane optionsBtn = buildSecondaryButton("⚙   Options");
        StackPane tutorialBtn = buildSecondaryButton("📖   Tutorial");
        StackPane changeUserBtn = buildSecondaryButton("👤   Change User");
        optionsBtn.setOnMouseClicked(e -> onOptionsClicked());
        tutorialBtn.setOnMouseClicked(e -> onTutorialClicked());
        changeUserBtn.setOnMouseClicked(e -> onChangeUserClicked());
        secondaryButtonsBox.getChildren().addAll(optionsBtn, tutorialBtn, changeUserBtn);

        StackPane quitBtn = buildQuitButton();
        quitBtn.setOnMouseClicked(e -> onQuitClicked());
        quitButtonContainer.getChildren().setAll(quitBtn);

        if (windowModeCombo != null) {
            windowModeCombo.setItems(FXCollections.observableArrayList(
                    "Windowed", "Windowed Fullscreen", "Fullscreen"));
            switch (Main.getWindowMode()) {
                case WINDOWED:
                    windowModeCombo.getSelectionModel().select(0);
                    break;
                case WINDOWED_FULLSCREEN:
                    windowModeCombo.getSelectionModel().select(1);
                    break;
                case FULLSCREEN:
                    windowModeCombo.getSelectionModel().select(2);
                    break;
            }
            windowModeCombo.setOnAction(e -> {
                int idx = windowModeCombo.getSelectionModel().getSelectedIndex();
                if (idx == 0)
                    Main.setWindowMode(Main.WindowMode.WINDOWED);
                else if (idx == 1)
                    Main.setWindowMode(Main.WindowMode.WINDOWED_FULLSCREEN);
                else
                    Main.setWindowMode(Main.WindowMode.FULLSCREEN);
            });
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onStartGameClicked() {
        SoundManager.playClick();
        Main.navigateTo("connect-view.fxml");
    }

    private void onPlayVsAiClicked() {
        SoundManager.playClick();
        Wizard player = new Wizard("Player", 200, Wizard.WizardSkin.ARCANE_WIZARD);
        Wizard ai = new Wizard("AI", 200, Wizard.WizardSkin.ARCANE_WIZARD);
        SoloGameController.setup(player, ai, new AiOpponent(new StandardAi()));
        Main.navigateTo("solo-game-view.fxml");
    }

    private void onOptionsClicked() {
        SoundManager.playClick();
        if (errorLabel != null)
            errorLabel.setText("Options coming soon.");
    }

    private void onTutorialClicked() {
        SoundManager.playClick();
        if (errorLabel != null)
            errorLabel.setText("Tutorial coming soon.");
    }

    private void onChangeUserClicked() {
        SoundManager.playClick();
        Main.navigateWithSlideRightTransition("profile-view.fxml");
    }

    private void onQuitClicked() {
        SoundManager.playClick();
        Stage stage = Main.getPrimaryStage();
        if (stage != null)
            stage.close();
        Platform.exit();
    }

    // ── Button builders ───────────────────────────────────────────────────────

    private StackPane buildPrimaryButton(String text, boolean isPurple) {
        String outerBg = isPurple ? "#4c3ba6" : "#7a5a18";

        String bgIdle = isPurple
                ? "linear-gradient(to bottom, #9888f5 0%, #9888f5 3px, #6e5cd4 3px, #5a48c0 100%)"
                : "linear-gradient(to bottom, #f5d080 0%, #f5d080 3px, #d4a035 3px, #c8922a 100%)";
        String borderIdle = isPurple
                ? "#c0b5ff #4c3ba6 #4c3ba6 #c0b5ff"
                : "#fce89a #a07020 #a07020 #fce89a";
        String textIdle = isPurple
                ? "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.5), 0, 0.0, 0, 1.5);"
                : "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a0d00; -fx-effect: dropshadow(one-pass-box, rgba(255,220,130,0.6), 0, 0.0, 0, 1);";

        String bgHover = isPurple
                ? "linear-gradient(to bottom, #b4a8ff 0%, #b4a8ff 3px, #8878ff 3px, #7a68e8 100%)"
                : "linear-gradient(to bottom, #ffe9a0 0%, #ffe9a0 3px, #f0c040 3px, #e8a828 100%)";
        String borderHover = isPurple
                ? "#dcd6ff #5e4cc4 #5e4cc4 #dcd6ff"
                : "#fff5c0 #c09030 #c09030 #fff5c0";

        String bgPressed = isPurple
                ? "linear-gradient(to bottom, #4432a6 0%, #4432a6 3px, #5a48c0 3px, #6e5cd4 100%)"
                : "linear-gradient(to bottom, #b07820 0%, #b07820 3px, #c8922a 3px, #d4a035 100%)";
        String borderPressed = isPurple
                ? "#2d1f7c #c0b5ff #c0b5ff #2d1f7c"
                : "#7a5010 #fce89a #fce89a #7a5010";

        Label label = new Label(text);
        label.setStyle(textIdle);
        label.setMouseTransparent(true);

        StackPane inner = new StackPane(label);
        inner.setPrefSize(326, 48);
        inner.setMinSize(326, 48);
        inner.setMaxSize(326, 48);
        StackPane.setAlignment(inner, Pos.TOP_LEFT);
        inner.setTranslateX(2);
        inner.setTranslateY(2);

        StackPane outerBevel = new StackPane(inner);
        outerBevel.setPrefSize(330, 52);
        outerBevel.setMinSize(330, 52);
        outerBevel.setMaxSize(330, 52);

        String outerStyle = "-fx-background-color: " + outerBg
                + "; -fx-background-radius: 0; -fx-cursor: hand; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);";
        String idleStyle = "-fx-background-color: " + bgIdle
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderIdle
                + "; -fx-border-width: 2px; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + bgHover
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderHover
                + "; -fx-border-width: 2px; -fx-cursor: hand;"
                + (isPurple
                        ? "-fx-effect: dropshadow(three-pass-box, rgba(152,136,245,0.22), 6, 0.1, 0, 0);"
                        : "-fx-effect: dropshadow(three-pass-box, rgba(255,200,60,0.18), 6, 0.1, 0, 0);");
        String pressedStyle = "-fx-background-color: " + bgPressed
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderPressed
                + "; -fx-border-width: 2px; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.5), 0, 0.0, 0, 0);";

        outerBevel.setStyle(outerStyle);
        inner.setStyle(idleStyle);

        // Scale-pulse on hover entry
        ScaleTransition hoverPulse = new ScaleTransition(Duration.millis(120), outerBevel);

        outerBevel.setOnMouseEntered(e -> {
            inner.setStyle(hoverStyle);
            hoverPulse.stop();
            hoverPulse.setFromX(outerBevel.getScaleX());
            hoverPulse.setFromY(outerBevel.getScaleY());
            hoverPulse.setToX(1.06);
            hoverPulse.setToY(1.06);
            hoverPulse.play();
        });
        outerBevel.setOnMouseExited(e -> {
            inner.setStyle(idleStyle);
            hoverPulse.stop();
            hoverPulse.setFromX(outerBevel.getScaleX());
            hoverPulse.setFromY(outerBevel.getScaleY());
            hoverPulse.setToX(1.0);
            hoverPulse.setToY(1.0);
            hoverPulse.play();
        });
        outerBevel.setOnMousePressed(e -> {
            inner.setStyle(pressedStyle);
            inner.setTranslateX(4);
            inner.setTranslateY(4);
        });
        outerBevel.setOnMouseReleased(e -> {
            inner.setTranslateX(2);
            inner.setTranslateY(2);
            if (outerBevel.isHover()) {
                inner.setStyle(hoverStyle);
            } else {
                inner.setStyle(idleStyle);
            }
        });

        return outerBevel;
    }

    private StackPane buildSecondaryButton(String text) {
        String outerBg = "#1c202a";

        String bgIdle = "linear-gradient(to bottom, #343d4d 0%, #343d4d 3px, #202631 3px, #181d26 100%)";
        String borderIdle = "#4f5c73 #12151c #12151c #4f5c73";
        String textIdle = "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #a0a0c0; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.8), 0, 0.0, 0, 1);";

        String bgHover = "linear-gradient(to bottom, #434e63 0%, #434e63 3px, #2a3240 3px, #1e2430 100%)";
        String borderHover = "#62738f #1a1e26 #1a1e26 #62738f";

        String bgPressed = "linear-gradient(to bottom, #12151c 0%, #12151c 3px, #181d26 3px, #202631 100%)";
        String borderPressed = "#12151c #4f5c73 #4f5c73 #12151c";

        Label label = new Label(text);
        label.setStyle(textIdle);
        label.setMouseTransparent(true);

        StackPane inner = new StackPane(label);
        inner.setPrefSize(246, 40);
        inner.setMinSize(246, 40);
        inner.setMaxSize(246, 40);
        StackPane.setAlignment(inner, Pos.TOP_LEFT);
        inner.setTranslateX(2);
        inner.setTranslateY(2);

        StackPane outerBevel = new StackPane(inner);
        outerBevel.setPrefSize(250, 44);
        outerBevel.setMinSize(250, 44);
        outerBevel.setMaxSize(250, 44);

        String outerStyle = "-fx-background-color: " + outerBg
                + "; -fx-background-radius: 0; -fx-cursor: hand; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);";
        String idleStyle = "-fx-background-color: " + bgIdle
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderIdle
                + "; -fx-border-width: 2px; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + bgHover
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderHover
                + "; -fx-border-width: 2px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(150,180,220,0.15), 6, 0.1, 0, 0);";
        String pressedStyle = "-fx-background-color: " + bgPressed
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderPressed
                + "; -fx-border-width: 2px; -fx-cursor: hand; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.5), 0, 0.0, 0, 0);";

        outerBevel.setStyle(outerStyle);
        inner.setStyle(idleStyle);

        // Scale-pulse on hover entry
        ScaleTransition hoverPulse = new ScaleTransition(Duration.millis(120), outerBevel);

        outerBevel.setOnMouseEntered(e -> {
            inner.setStyle(hoverStyle);
            hoverPulse.stop();
            hoverPulse.setFromX(outerBevel.getScaleX());
            hoverPulse.setFromY(outerBevel.getScaleY());
            hoverPulse.setToX(1.06);
            hoverPulse.setToY(1.06);
            hoverPulse.play();
        });
        outerBevel.setOnMouseExited(e -> {
            inner.setStyle(idleStyle);
            hoverPulse.stop();
            hoverPulse.setFromX(outerBevel.getScaleX());
            hoverPulse.setFromY(outerBevel.getScaleY());
            hoverPulse.setToX(1.0);
            hoverPulse.setToY(1.0);
            hoverPulse.play();
        });
        outerBevel.setOnMousePressed(e -> {
            inner.setStyle(pressedStyle);
            inner.setTranslateX(4);
            inner.setTranslateY(4);
        });
        outerBevel.setOnMouseReleased(e -> {
            inner.setTranslateX(2);
            inner.setTranslateY(2);
            if (outerBevel.isHover()) {
                inner.setStyle(hoverStyle);
            } else {
                inner.setStyle(idleStyle);
            }
        });

        return outerBevel;
    }

    private StackPane buildQuitButton() {
        Label label = new Label("— Quit —");
        label.setStyle(
                "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 12px; -fx-text-fill: #506275; -fx-cursor: hand;");
        label.setMouseTransparent(true);

        StackPane btn = new StackPane(label);
        btn.setPrefSize(120, 28);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        // Scale-pulse on hover entry
        ScaleTransition hoverPulse = new ScaleTransition(Duration.millis(120), btn);

        btn.setOnMouseEntered(e -> {
            label.setStyle(
                    "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 12px; -fx-text-fill: #ff6b6b; -fx-cursor: hand;");
            hoverPulse.stop();
            hoverPulse.setFromX(btn.getScaleX());
            hoverPulse.setFromY(btn.getScaleY());
            hoverPulse.setToX(1.08);
            hoverPulse.setToY(1.08);
            hoverPulse.play();
        });
        btn.setOnMouseExited(e -> {
            label.setStyle(
                    "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 12px; -fx-text-fill: #506275; -fx-cursor: hand;");
            hoverPulse.stop();
            hoverPulse.setFromX(btn.getScaleX());
            hoverPulse.setFromY(btn.getScaleY());
            hoverPulse.setToX(1.0);
            hoverPulse.setToY(1.0);
            hoverPulse.play();
        });
        btn.setOnMousePressed(e -> {
            label.setStyle(
                    "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 12px; -fx-text-fill: #cc3333; -fx-cursor: hand;");
            label.setTranslateY(1);
        });
        btn.setOnMouseReleased(e -> {
            label.setTranslateY(0);
            if (btn.isHover()) {
                label.setStyle(
                        "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 12px; -fx-text-fill: #ff6b6b; -fx-cursor: hand;");
            } else {
                label.setStyle(
                        "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 12px; -fx-text-fill: #506275; -fx-cursor: hand;");
            }
        });

        return btn;
    }
}