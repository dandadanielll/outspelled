package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.util.SoundManager;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class LandingController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImageView;
    @FXML private Pane weatherPane;

    private final List<Circle> starCluster = new ArrayList<>();
    private long lastStarUpdate = 0;
    private boolean isTransitioning = false; // Prevents double-triggering inputs

    @FXML
    public void initialize() {
        // Load custom font first because JavaFX does not support @font-face in CSS natively
        java.net.URL fontUrl = LandingController.class.getResource("/assets/fonts/PixelifySans-VariableFont_wght.ttf");
        if (fontUrl != null) {
            Font.loadFont(fontUrl.toExternalForm(), 12);
        }

        SoundManager.startBgm("bgm.mp3");

        java.net.URL imgUrl = LandingController.class.getResource("/assets/Landing-background.png");
        if (imgUrl != null) {
            backgroundImageView.setImage(new Image(imgUrl.toExternalForm()));
        }

        backgroundImageView.setSmooth(false);
        backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());

        setupAtmosphereEffects();

        // Key Listeners
        rootPane.setFocusTraversable(true);
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

        // FIX: Run later guarantees the stage is open and window focus context is stable
        Platform.runLater(() -> {
            rootPane.requestFocus();
        });
    }

    private void setupAtmosphereEffects() {
        // Moon Glow
        RadialGradient moonGlowGradient = new RadialGradient(
            0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#fffdd0", 0.18)), 
            new Stop(1, Color.TRANSPARENT)
        );

        Circle moonGlow = new Circle(180, moonGlowGradient);
        moonGlow.translateXProperty().bind(rootPane.widthProperty().multiply(0.06)); 
        moonGlow.translateYProperty().bind(rootPane.heightProperty().multiply(-0.22));

        FadeTransition moonPulse = new FadeTransition(Duration.seconds(4.0), moonGlow);
        moonPulse.setFromValue(0.3);
        moonPulse.setToValue(1.0);
        moonPulse.setAutoReverse(true);
        moonPulse.setCycleCount(Animation.INDEFINITE);
        moonPulse.play();
        
        weatherPane.getChildren().add(moonGlow);

        // 80 Stars
        generateStars(80);

        AnimationTimer coreGameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastStarUpdate > 120_000_000) {
                    for (Circle star : starCluster) {
                        if (Math.random() < 0.15) {
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
            double radius = (Math.random() > 0.75) ? 1.5 : 1.0;
            Circle star = new Circle(radius, Color.web("#fffdd0", 0.75));
            star.layoutXProperty().bind(rootPane.widthProperty().multiply(Math.random()));
            star.layoutYProperty().bind(rootPane.heightProperty().multiply(Math.random() * 0.38)); 
            weatherPane.getChildren().add(star);
            starCluster.add(star);
        }
    }

    @FXML
    private void onEnterClicked() {
        // Guard check ensures multiple fast keypresses don't crash the scene manager
        if (isTransitioning) return;
        isTransitioning = true;

        SoundManager.playClick();
        
        FadeTransition ft = new FadeTransition(Duration.millis(450), rootPane);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> Main.navigateTo("profile-view.fxml"));
        ft.play();
    }
}