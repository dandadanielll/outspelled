package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.util.SoundManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class LandingController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImageView;
    @FXML private Button enterButton;

    @FXML
    public void initialize() {
        // Start background music immediately
        SoundManager.startBgm("bgm.mp3");

        // Load background image
        java.net.URL imgUrl = LandingController.class.getResource("/assets/Landing-background.png");
        if (imgUrl != null) {
            backgroundImageView.setImage(new Image(imgUrl.toExternalForm()));
        }

        // CRUCIAL FOR PIXEL ART: Tells JavaFX not to blur your upscaled castle art
        backgroundImageView.setSmooth(false);

        // Bind image dimensions to root pane size for responsiveness
        backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());

        // Dynamic Hover effect on Enter Button (Updated to sharp square edges for a retro aesthetic)
        String idleStyle = "-fx-background-color: #e2b96f; " +
                           "-fx-text-fill: #1a1a2e; " +
                           "-fx-font-size: 18px; " + // Cleaned up scale balance slightly for left-alignment
                           "-fx-font-weight: bold; " +
                           "-fx-font-family: 'Georgia'; " +
                           "-fx-padding: 14 50; " +
                           "-fx-background-radius: 0; " + // Clean, crisp corner profile
                           "-fx-border-radius: 0; " +
                           "-fx-cursor: hand; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 10, 0.5, 0, 4);";

        String hoverStyle = "-fx-background-color: #f7d69e; " +
                            "-fx-text-fill: #1a1a2e; " +
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-family: 'Georgia'; " +
                            "-fx-padding: 14 50; " +
                            "-fx-background-radius: 0; " + 
                            "-fx-border-radius: 0; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(226, 185, 111, 0.5), 15, 0.5, 0, 0);";

        // Assign default style initially
        enterButton.setStyle(idleStyle);

        enterButton.setOnMouseEntered(e -> {
            enterButton.setStyle(hoverStyle);
            // Pulse micro-animation on hover
            ScaleTransition st = new ScaleTransition(Duration.millis(150), enterButton);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        enterButton.setOnMouseExited(e -> {
            enterButton.setStyle(idleStyle);
            ScaleTransition st = new ScaleTransition(Duration.millis(150), enterButton);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        // Let pressing ENTER or SPACE fire the click action as well
        rootPane.setFocusTraversable(true);
        rootPane.requestFocus();
        rootPane.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                case SPACE:
                    onEnterClicked();
                    break;
                default:
                    break;
            }
        });
    }

    @FXML
    private void onEnterClicked() {
        SoundManager.playClick();
        
        // Smooth fade-out animation before navigating
        FadeTransition ft = new FadeTransition(Duration.millis(450), rootPane);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> Main.navigateTo("profile-view.fxml"));
        ft.play();
    }
}