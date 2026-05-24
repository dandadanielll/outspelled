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
import javafx.scene.layout.HBox;
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
        showSettingsDialog();
    }

    private void showSettingsDialog() {
        Label headerLabel = styledDialogLabel("Adjust Game Volumes");
        headerLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-text-fill: #8899aa;");

        // BGM Slider
        Label bgmLabel = new Label("Background Music");
        bgmLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 14px; -fx-text-fill: #e2b96f;");
        javafx.scene.control.Slider bgmSlider = new javafx.scene.control.Slider(0, 1.0, SoundManager.getBgmVolume());
        bgmSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            SoundManager.setBgmVolume(newVal.doubleValue());
        });

        // SFX Slider
        Label sfxLabel = new Label("Sound Effects");
        sfxLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 14px; -fx-text-fill: #e2b96f;");
        javafx.scene.control.Slider sfxSlider = new javafx.scene.control.Slider(0, 1.0, SoundManager.getSfxVolume());
        sfxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            SoundManager.setSfxVolume(newVal.doubleValue());
        });

        sfxSlider.setOnMouseReleased(e -> SoundManager.playClick());

        VBox bgmBox = new VBox(5, bgmLabel, bgmSlider);
        VBox sfxBox = new VBox(5, sfxLabel, sfxSlider);
        bgmBox.setAlignment(Pos.CENTER);
        sfxBox.setAlignment(Pos.CENTER);

        StackPane closeBtn = buildDialogButton("Close", true);
        closeBtn.setOnMouseClicked(e -> {
            SoundManager.playClick();
            com.rst.outspelled.util.OverlayManager.hideOverlay();
        });

        VBox body = new VBox(24);
        body.setStyle("-fx-padding: 30 24 24 24;");
        body.setAlignment(Pos.CENTER);
        body.getChildren().addAll(headerLabel, bgmBox, sfxBox, closeBtn);

        VBox dialogRoot = buildDialogRoot("⚙  Options  ⚙", false, body, 400, 320);
        com.rst.outspelled.util.OverlayManager.showOverlay(dialogRoot);
    }

    private void onTutorialClicked() {
        SoundManager.playClick();

        final int[] currentStep = { 0 };

        final String[] TUTORIAL_DESCRIPTIONS = {
                "Spell words using your letter tiles to cast powerful spells!",
                "Collect mana crystals to charge your wizard's ultimate abilities.",
                "Defeat your opponent in wizard duels to earn experience and wins!"
        };

        final String[] TUTORIAL_IMAGES = {
                "/assets/tutorial/Step-1.png",
                "/assets/tutorial_step2.png",
                "/assets/tutorial_step3.png"
        };

        // Header label
        Label headerLabel = styledDialogLabel("Learn how to duel like a true Archmage!");
        headerLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-text-fill: #8899aa;");

        // The image/visual viewport container
        StackPane visualContainer = new StackPane();
        visualContainer.setPrefSize(420, 264);
        visualContainer.setMinSize(420, 264);
        visualContainer.setMaxSize(420, 264);
        visualContainer.setStyle(
                "-fx-border-color: #e2b96f;" +
                        "-fx-border-width: 2px;" +
                        "-fx-background-color: #0d0f14;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(226, 185, 111, 0.15), 10, 0.5, 0, 0);");

        // Stepper buttons styled as premium bevel pixel buttons
        StackPane leftArrow = buildArrowButton("◀");
        StackPane rightArrow = buildArrowButton("▶");

        HBox stepperRow = new HBox(16);
        stepperRow.setAlignment(Pos.CENTER);
        stepperRow.getChildren().addAll(leftArrow, visualContainer, rightArrow);

        // Text description below image
        Label descLabel = new Label();
        descLabel.getStyleClass().add("pixel-font");
        descLabel.setWrapText(true);
        descLabel.setPrefWidth(460);
        descLabel.setMinHeight(48);
        descLabel.setMaxHeight(48);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setStyle(
                "-fx-text-fill: #c0cce0;" +
                        "-fx-font-family: 'Pixelify Sans';" +
                        "-fx-font-size: 14px;" +
                        "-fx-text-alignment: center;");

        // Got it close button
        StackPane gotItBtn = buildDialogButton("Got it!", true);
        gotItBtn.setOnMouseClicked(e -> { SoundManager.playClick(); com.rst.outspelled.util.OverlayManager.hideOverlay(); });

        HBox footerRow = new HBox(gotItBtn);
        footerRow.setAlignment(Pos.CENTER);

        VBox body = new VBox(16);
        body.setStyle("-fx-padding: 20 24 16 24;");
        body.setAlignment(Pos.CENTER);
        body.getChildren().addAll(
                headerLabel,
                stepperRow,
                descLabel,
                footerRow);

        // Update step behavior
        Runnable updateStep = () -> {
            int step = currentStep[0];
            descLabel.setText(TUTORIAL_DESCRIPTIONS[step]);

            java.net.URL imgUrl = MenuController.class.getResource(TUTORIAL_IMAGES[step]);
            visualContainer.getChildren().clear();
            if (imgUrl != null) {
                try {
                    javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(
                            new javafx.scene.image.Image(imgUrl.toExternalForm()));
                    imgView.setFitWidth(416);
                    imgView.setFitHeight(260);
                    imgView.setPreserveRatio(true);
                    visualContainer.getChildren().add(imgView);
                } catch (Exception ex) {
                    showPlaceholderVisual(visualContainer, step);
                }
            } else {
                showPlaceholderVisual(visualContainer, step);
            }

            boolean isFirst = step == 0;
            boolean isLast = step == TUTORIAL_DESCRIPTIONS.length - 1;
            setArrowButtonDisabled(leftArrow, isFirst);
            setArrowButtonDisabled(rightArrow, isLast);
        };

        leftArrow.setOnMouseClicked(e -> {
            SoundManager.playClick();
            if (currentStep[0] > 0) {
                currentStep[0]--;
                updateStep.run();
            }
        });

        rightArrow.setOnMouseClicked(e -> {
            SoundManager.playClick();
            if (currentStep[0] < TUTORIAL_DESCRIPTIONS.length - 1) {
                currentStep[0]++;
                updateStep.run();
            }
        });

        // Initial setup
        updateStep.run();

        VBox dialogRoot = buildDialogRoot("✦  Wizard Academy  ✦", false, body, 560, 460);
        com.rst.outspelled.util.OverlayManager.showOverlay(dialogRoot);
    }

    private StackPane buildArrowButton(String text) {
        StackPane outerBevel = new StackPane();
        outerBevel.setPrefSize(38, 38);
        outerBevel.setMinSize(38, 38);
        outerBevel.setMaxSize(38, 38);

        String outerBg = "#1f2430";
        outerBevel.setStyle(
                "-fx-background-color: " + outerBg + ";" +
                        "-fx-background-radius: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.5), 0, 0.0, 3, 3);");

        String bgIdle = "linear-gradient(to bottom, #728198 0%, #728198 3px, #424e61 3px, #364050 100%)";
        String borderIdle = "#9bb0cc #262e3b #262e3b #9bb0cc";
        String textStyle = "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0a0d14; -fx-effect: dropshadow(one-pass-box, rgba(200,220,255,0.4), 0, 0.0, 0, 1);";

        String bgHover = "linear-gradient(to bottom, #8496b0 0%, #8496b0 3px, #4f5d75 3px, #414d61 100%)";
        String borderHover = "#adccf5 #303b4d #303b4d #adccf5";
        String effectHover = "dropshadow(three-pass-box, rgba(150,180,220,0.15), 6, 0.1, 0, 0)";

        String bgPressed = "linear-gradient(to bottom, #2b3340 0%, #2b3340 3px, #424e61 3px, #4d5b70 100%)";
        String borderPressed = "#1a2029 #9bb0cc #9bb0cc #1a2029";

        Label label = new Label(text);
        label.setStyle(textStyle);
        label.setMouseTransparent(true);

        StackPane inner = new StackPane(label);
        inner.setPrefSize(34, 34);
        inner.setMinSize(34, 34);
        inner.setMaxSize(34, 34);

        String idleStyle = "-fx-background-color: " + bgIdle
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderIdle
                + "; -fx-border-width: 2px; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + bgHover
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderHover
                + "; -fx-border-width: 2px; -fx-cursor: hand; -fx-effect: " + effectHover + ";";
        String pressedStyle = "-fx-background-color: " + bgPressed
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderPressed
                + "; -fx-border-width: 2px; -fx-cursor: hand;";

        inner.setStyle(idleStyle);

        StackPane.setAlignment(inner, Pos.TOP_LEFT);
        inner.setTranslateX(2);
        inner.setTranslateY(2);

        outerBevel.getChildren().add(inner);

        outerBevel.setOnMouseEntered(e -> {
            if (!outerBevel.isDisable()) {
                inner.setStyle(hoverStyle);
            }
        });
        outerBevel.setOnMouseExited(e -> {
            if (!outerBevel.isDisable()) {
                inner.setStyle(idleStyle);
            }
        });
        outerBevel.setOnMousePressed(e -> {
            if (!outerBevel.isDisable()) {
                inner.setStyle(pressedStyle);
                inner.setTranslateX(4);
                inner.setTranslateY(4);
            }
        });
        outerBevel.setOnMouseReleased(e -> {
            if (!outerBevel.isDisable()) {
                inner.setTranslateX(2);
                inner.setTranslateY(2);
                if (outerBevel.isHover()) {
                    inner.setStyle(hoverStyle);
                } else {
                    inner.setStyle(idleStyle);
                }
            }
        });

        return outerBevel;
    }

    private void setArrowButtonDisabled(StackPane btn, boolean disabled) {
        btn.setDisable(disabled);
        btn.setOpacity(disabled ? 0.35 : 1.0);
        StackPane inner = (StackPane) btn.getChildren().get(0);
        if (disabled) {
            String bgDisabled = "linear-gradient(to bottom, #4f5a6b 0%, #4f5a6b 3px, #2a323d 3px, #20262f 100%)";
            String borderDisabled = "#6b7c94 #12151a #12151a #6b7c94";
            inner.setStyle("-fx-background-color: " + bgDisabled + "; -fx-border-color: " + borderDisabled
                    + "; -fx-border-width: 2px; -fx-background-radius: 0; -fx-border-radius: 0;");
        } else {
            String bgIdle = "linear-gradient(to bottom, #728198 0%, #728198 3px, #424e61 3px, #364050 100%)";
            String borderIdle = "#9bb0cc #262e3b #262e3b #9bb0cc";
            inner.setStyle("-fx-background-color: " + bgIdle + "; -fx-border-color: " + borderIdle
                    + "; -fx-border-width: 2px; -fx-background-radius: 0; -fx-border-radius: 0; -fx-cursor: hand;");
        }
    }

    private void showPlaceholderVisual(StackPane container, int step) {
        String[] icons = { "[ * ]", "[ ~ ]", "[ ! ]" };
        String[] names = { "Spelling & Casts", "Mana Crystals", "Glory & Victory" };
        String[] colors = { "#a38aff", "#6ec6ff", "#e2b96f" };

        Label iconLabel = new Label(icons[step]);
        iconLabel.setStyle(
                "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 28px; -fx-text-fill: " + colors[step] + ";" +
                        "-fx-effect: dropshadow(three-pass-box, " + colors[step] + ", 8, 0.5, 0, 0);");

        Label stepLabel = new Label("Step " + (step + 1) + " of 3");
        stepLabel.setStyle(
                "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 10px; -fx-text-fill: #4a5a70;");

        Label nameLabel = new Label(names[step]);
        nameLabel.setStyle(
                "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                        + colors[step] + ";");

        VBox box = new VBox(6, iconLabel, stepLabel, nameLabel);
        box.setAlignment(Pos.CENTER);
        container.getChildren().add(box);
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

    // ── Dialog builders and helpers matching ProfileController.java ───────────

    private VBox buildDialogRoot(String titleText, boolean danger, VBox body, double width, double height) {
        Label titleLabel = new Label(titleText);
        titleLabel.setStyle(
                "-fx-font-family: 'Pixelify Sans';" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + (danger ? "#ff6b6b" : "#e2b96f") + ";");

        HBox titleBar = new HBox(titleLabel);
        titleBar.setAlignment(Pos.CENTER);
        titleBar.setStyle(
                "-fx-background-color: #0f1220;" +
                        "-fx-padding: 12 20;" +
                        "-fx-border-color: transparent transparent " +
                        (danger ? "#cc3333" : "#e8c97a") + " transparent;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-border-opacity: 0.5;");

        VBox inner = new VBox(0, titleBar, body);
        inner.setStyle(
                "-fx-background-color: #1a1e2e;" +
                        "-fx-border-color: " + (danger ? "#cc3333" : "#e8c97a") + ";" +
                        "-fx-border-width: 2px;");

        javafx.scene.shape.Rectangle notchTL = dialogNotch(0, 0);
        javafx.scene.shape.Rectangle notchTR = dialogNotch(-4, 0);
        javafx.scene.shape.Rectangle notchBL = dialogNotch(0, -4);
        javafx.scene.shape.Rectangle notchBR = dialogNotch(-4, -4);
        StackPane.setAlignment(notchTR, Pos.TOP_RIGHT);
        StackPane.setAlignment(notchBL, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(notchBR, Pos.BOTTOM_RIGHT);

        StackPane root = new StackPane(inner, notchTL, notchTR, notchBL, notchBR);
        root.setPrefSize(width, height);
        root.setMaxSize(width, height);
        root.setStyle("-fx-background-color: transparent;");

        VBox outerShell = new VBox(root);
        outerShell.setStyle(
                "-fx-background-color: transparent; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);");
        outerShell.setAlignment(Pos.CENTER);
        return outerShell;
    }

    private javafx.scene.shape.Rectangle dialogNotch(double x, double y) {
        javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(4, 4);
        r.setFill(javafx.scene.paint.Color.web("#12151e"));
        if (x != 0)
            StackPane.setMargin(r, new javafx.geometry.Insets(0, Math.abs(x), 0, 0));
        if (y != 0)
            StackPane.setMargin(r, new javafx.geometry.Insets(0, 0, Math.abs(y), 0));
        return r;
    }

    private Label styledDialogLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 12px; -fx-text-fill: #8899aa;");
        return l;
    }

    private StackPane buildDialogButton(String text, boolean isPrimary) {
        return buildDialogButton(text, isPrimary, false);
    }

    private StackPane buildDialogButton(String text, boolean isPrimary, boolean danger) {
        StackPane outerBevel = new StackPane();
        outerBevel.setPrefSize(130, 36);
        outerBevel.setMinSize(130, 36);
        outerBevel.setMaxSize(130, 36);

        String outerBg = danger ? "#5a0f0f" : (isPrimary ? "#7a5a18" : "#1f2430");
        outerBevel.setStyle(
                "-fx-background-color: " + outerBg + ";" +
                        "-fx-background-radius: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.5), 0, 0.0, 4, 4);");

        String bgIdle, borderIdle, textStyle;
        String bgHover, borderHover, effectHover;
        String bgPressed, borderPressed;

        if (danger) {
            bgIdle = "linear-gradient(to bottom, #ff9494 0%, #ff9494 3px, #cc3333 3px, #b32424 100%)";
            borderIdle = "#ffc4c4 #8c1c1c #8c1c1c #ffc4c4";
            bgHover = "linear-gradient(to bottom, #ffadad 0%, #ffadad 3px, #e63939 3px, #cc2929 100%)";
            borderHover = "#ffe0e0 #a62222 #a62222 #ffe0e0";
            effectHover = "dropshadow(three-pass-box, rgba(255,100,100,0.22), 6, 0.1, 0, 0)";
            bgPressed = "linear-gradient(to bottom, #8a1515 0%, #8a1515 3px, #cc3333 3px, #d94141 100%)";
            borderPressed = "#5c0c0c #ffc4c4 #ffc4c4 #5c0c0c";
            textStyle = "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #200505; -fx-effect: dropshadow(one-pass-box, rgba(255,180,180,0.6), 0, 0.0, 0, 1);";
        } else if (isPrimary) {
            bgIdle = "linear-gradient(to bottom, #f5d080 0%, #f5d080 3px, #d4a035 3px, #c8922a 100%)";
            borderIdle = "#fce89a #a07020 #a07020 #fce89a";
            bgHover = "linear-gradient(to bottom, #ffe9a0 0%, #ffe9a0 3px, #f0c040 3px, #e8a828 100%)";
            borderHover = "#fff5c0 #c09030 #c09030 #fff5c0";
            effectHover = "dropshadow(three-pass-box, rgba(255,200,60,0.18), 6, 0.1, 0, 0)";
            bgPressed = "linear-gradient(to bottom, #b07820 0%, #b07820 3px, #c8922a 3px, #d4a035 100%)";
            borderPressed = "#7a5010 #fce89a #fce89a #7a5010";
            textStyle = "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a0d00; -fx-effect: dropshadow(one-pass-box, rgba(255,220,130,0.6), 0, 0.0, 0, 1);";
        } else {
            bgIdle = "linear-gradient(to bottom, #728198 0%, #728198 3px, #424e61 3px, #364050 100%)";
            borderIdle = "#9bb0cc #262e3b #262e3b #9bb0cc";
            bgHover = "linear-gradient(to bottom, #8496b0 0%, #8496b0 3px, #4f5d75 3px, #414d61 100%)";
            borderHover = "#adccf5 #303b4d #303b4d #adccf5";
            effectHover = "dropshadow(three-pass-box, rgba(150,180,220,0.15), 6, 0.1, 0, 0)";
            bgPressed = "linear-gradient(to bottom, #2b3340 0%, #2b3340 3px, #424e61 3px, #4d5b70 100%)";
            borderPressed = "#1a2029 #9bb0cc #9bb0cc #1a2029";
            textStyle = "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0a0d14; -fx-effect: dropshadow(one-pass-box, rgba(200,220,255,0.4), 0, 0.0, 0, 1);";
        }

        Label label = new Label(text);
        label.setStyle(textStyle);
        label.setMouseTransparent(true);

        StackPane inner = new StackPane(label);
        inner.setPrefSize(126, 32);
        inner.setMinSize(126, 32);
        inner.setMaxSize(126, 32);

        String idleStyle = "-fx-background-color: " + bgIdle
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderIdle
                + "; -fx-border-width: 2px; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + bgHover
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderHover
                + "; -fx-border-width: 2px; -fx-cursor: hand; -fx-effect: " + effectHover + ";";
        String pressedStyle = "-fx-background-color: " + bgPressed
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderPressed
                + "; -fx-border-width: 2px; -fx-cursor: hand;";

        inner.setStyle(idleStyle);

        StackPane.setAlignment(inner, Pos.TOP_LEFT);
        inner.setTranslateX(2);
        inner.setTranslateY(2);

        outerBevel.getChildren().add(inner);

        outerBevel.setOnMouseEntered(e -> inner.setStyle(hoverStyle));
        outerBevel.setOnMouseExited(e -> inner.setStyle(idleStyle));
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
        return buildSecondaryButton("🚪   Quit");
    }
}