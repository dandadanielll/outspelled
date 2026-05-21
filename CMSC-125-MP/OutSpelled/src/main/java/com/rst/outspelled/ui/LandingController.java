package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.util.SoundManager;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class LandingController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImageView;
    @FXML private Button enterButton;
    @FXML private Pane weatherPane; // Bound container layer from FXML

    // Star Tracking Parameters
    private final List<Circle> starCluster = new ArrayList<>();
    private long lastStarUpdate = 0;

    @FXML
    public void initialize() {
        // Maintain Native Audio System
        SoundManager.startBgm("bgm.mp3");

        // Load bg image
        java.net.URL imgUrl = LandingController.class.getResource("/assets/Landing-background.png");
        if (imgUrl != null) {
            backgroundImageView.setImage(new Image(imgUrl.toExternalForm()));
        }

        // Prevents JavaFX from smoothing/blurring my pixel art
        backgroundImageView.setSmooth(false);

        backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());

        // Initialize atmospheric system layers
        setupAtmosphereEffects();

        // Button styling
        String idleStyle = "-fx-background-color: #e2b96f; " +
                           "-fx-text-fill: #1a1a2e; " +
                           "-fx-font-size: 18px; " +
                           "-fx-font-weight: bold; " +
                           "-fx-font-family: 'Pixelify Sans', 'Georgia'; " +
                           "-fx-padding: 14 50; " +
                           "-fx-background-radius: 0; " + 
                           "-fx-border-radius: 0; " +
                           "-fx-cursor: hand; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 10, 0.5, 0, 4);";

        String hoverStyle = "-fx-background-color: #f7d69e; " +
                            "-fx-text-fill: #1a1a2e; " +
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-font-family: 'Pixelify Sans', 'Georgia'; " +
                            "-fx-padding: 14 50; " +
                            "-fx-background-radius: 0; " + 
                            "-fx-border-radius: 0; " +
                            "-fx-cursor: hand; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(226, 185, 111, 0.5), 15, 0.5, 0, 0);";

        enterButton.setStyle(idleStyle);

        enterButton.setOnMouseEntered(e -> {
            enterButton.setStyle(hoverStyle);
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

        // Global keyboard inputs
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

    private void setupAtmosphereEffects() {
        // THE PULSING MOON GLOW MASK
        RadialGradient moonGlowGradient = new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#fffdd0", 0.18)),
            new Stop(1, Color.TRANSPARENT)
        );

        Circle moonGlow = new Circle(180, moonGlowGradient);
        
        // Pin mask coordinates relative to my composition's right-centered moon
        moonGlow.translateXProperty().bind(rootPane.widthProperty().multiply(0.06)); 
        moonGlow.translateYProperty().bind(rootPane.heightProperty().multiply(-0.22));

        FadeTransition moonPulse = new FadeTransition(Duration.seconds(4.0), moonGlow);
        moonPulse.setFromValue(0.3);
        moonPulse.setToValue(1.0);
        moonPulse.setAutoReverse(true);
        moonPulse.setCycleCount(Animation.INDEFINITE);
        moonPulse.play();
        
        weatherPane.getChildren().add(moonGlow);

        // TWINKLING SKY CELESTIAL STARRY CLUSTER (80 for now, increase if too empty)
        generateStars(80);

        // CORE GAME ENGINE TIMELINE LOOP
        AnimationTimer coreGameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Evaluate Star Twinkles (Fires every 120ms block to save system resources)
                if (now - lastStarUpdate > 120_000_000) {
                    for (Circle star : starCluster) {
                        if (Math.random() < 0.15) { // Random chance threshold to shift opacity scale
                            star.setOpacity(0.15 + Math.random() * 0.85);
                        }
                    }
                    lastStarUpdate = now;
                }
            }
        };
        coreGameLoop.start();
    }

    private void generateStars(int count) {
        for (int i = 0; i < count; i++) {
            // Retro size profiles: Crisp 1.0px points, occasional 1.5px major bodies
            double radius = (Math.random() > 0.75) ? 1.5 : 1.0;
            Circle star = new Circle(radius, Color.web("#fffdd0", 0.75));

            // Smart procedural positioning binds stars to the clear upper atmospheric quadrants
            star.layoutXProperty().bind(rootPane.widthProperty().multiply(Math.random()));
            star.layoutYProperty().bind(rootPane.heightProperty().multiply(Math.random() * 0.38)); // Caps height within sky box limits

            weatherPane.getChildren().add(star);
            starCluster.add(star);
        }
    }

    @FXML
    private void onEnterClicked() {
        SoundManager.playClick();
        
        FadeTransition ft = new FadeTransition(Duration.millis(450), rootPane);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> Main.navigateTo("profile-view.fxml"));
        ft.play();
    }
}