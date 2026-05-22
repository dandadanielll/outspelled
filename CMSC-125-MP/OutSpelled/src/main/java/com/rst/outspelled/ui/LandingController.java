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
import javafx.animation.TranslateTransition;

import java.util.ArrayList;
import java.util.List;

public class LandingController {

    @FXML private StackPane rootPane;
    @FXML private ImageView backgroundImageView;
    @FXML private Pane weatherPane;

    //Animation Properties
    private final List<Circle> starCluster = new ArrayList<>(); //array for every star instantiated
    private long lastStarUpdate = 0;                            //timestamp for star updates
    private boolean isTransitioning = false;                    // Prevents double-triggering inputs

    @FXML
    public void initialize() {
        // Load custom font first because JavaFX does not support @font-face in CSS natively
        java.net.URL fontUrl = LandingController.class.getResource("/assets/fonts/PixelifySans-VariableFont_wght.ttf");
        if (fontUrl != null) {
            Font.loadFont(fontUrl.toExternalForm(), 12);
        }

        SoundManager.startBgm("bgm.mp3");

        //Setting up the background image
        java.net.URL imgUrl = LandingController.class.getResource("/assets/Landing-background.png");
        if (imgUrl != null) {
            backgroundImageView.setImage(new Image(imgUrl.toExternalForm()));
        }

        //Prevent javaFX from smoothing out the pixel art
        backgroundImageView.setSmooth(false);
        backgroundImageView.fitWidthProperty().bind(rootPane.widthProperty());
        backgroundImageView.fitHeightProperty().bind(rootPane.heightProperty());

        setupAtmosphereEffects();

        // Keyboard Event Listeners for spacebar and enter
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

        // Forces focus onto the rootPane once the window is fully open and active
        Platform.runLater(() -> {
            rootPane.requestFocus();
        });
    }

    private void setupAtmosphereEffects() {
        // Moon Glow Overlay
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

        // Generates 80 stars 
        generateStars(80);

        //Animation timer for the stars
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
            //star size randomizer, makes only 25% of the stars bigger
            double radius = (Math.random() > 0.75) ? 1.5 : 1.0;
            //setting the initial appearance of the stars
            Circle star = new Circle(radius, Color.web("#fffdd0", 0.75));
            //randomize x and y positions
            star.layoutXProperty().bind(rootPane.widthProperty().multiply(Math.random()));
            //limit the stars to top 38% of the screen
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
        
        // Navigate to the next view with a slide-up transition for both screens
        Main.navigateWithSlideUpTransition("profile-view.fxml");
    }
}