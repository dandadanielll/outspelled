package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.network.SessionManager;
import com.rst.outspelled.util.ProfileManager;
import com.rst.outspelled.util.SoundManager;
import javafx.animation.ScaleTransition;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;

public class ProfileController {

    @FXML
    private HBox profileSlotsBox;
    @FXML
    private Label statusLabel;
    @FXML
    private VBox playButtonContainer;

    // which slot is currently selected (-1 = none)
    private int selectedSlot = -1;
    private List<Wizard> profiles;

    // The play button — built as a plain StackPane so Modena CSS never touches it
    private StackPane playButton;

    @FXML
    public void initialize() {
        profiles = ProfileManager.loadAll();
        buildPlayButton();
        renderSlots();
    }

    // Builds the play button as a StackPane + Label so JavaFX's Button skin
    // (and Modena's stylesheet) never gets involved. Hover/press states are
    // applied via inline setStyle only, which always wins.
    private void buildPlayButton() {
        Label playLabel = new Label("▶   Play!");
        playLabel.setStyle(
                "-fx-font-family: 'Pixelify Sans';" +
                        "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #1a1000;" +
                        "-fx-effect: dropshadow(one-pass-box, rgba(255,220,130,0.6), 0, 0.0, 0, 1);" +
                        "-fx-cursor: hand;");
        playLabel.setMouseTransparent(true);

        // Outer wrapper carries the bottom-right dark bevel shadow
        StackPane outerBevel = new StackPane();
        outerBevel.setStyle(
                "-fx-background-color: #7a5a18;" + // dark bevel face
                        "-fx-background-radius: 0;");
        outerBevel.setMinSize(240, 58);
        outerBevel.setPrefSize(240, 58);
        outerBevel.setMaxSize(240, 58);

        // Inner face — the bright gold surface that shifts on press
        playButton = new StackPane(playLabel);
        applyPlayIdle();
        playButton.setMinSize(236, 52);
        playButton.setPrefSize(236, 52);
        playButton.setMaxSize(236, 52);
        // Sit the face 2px up / 2px left inside the dark bevel
        StackPane.setAlignment(playButton, Pos.TOP_LEFT);
        playButton.setTranslateX(2);
        playButton.setTranslateY(2);

        outerBevel.getChildren().add(playButton);

        // Scale-pulse on hover entry
        ScaleTransition hoverPulse = new ScaleTransition(Duration.millis(120), outerBevel);

        outerBevel.setOnMouseEntered(e -> {
            applyPlayHover();
            hoverPulse.stop();
            hoverPulse.setFromX(outerBevel.getScaleX());
            hoverPulse.setFromY(outerBevel.getScaleY());
            hoverPulse.setToX(1.06);
            hoverPulse.setToY(1.06);
            hoverPulse.play();
        });
        outerBevel.setOnMouseExited(e -> {
            applyPlayIdle();
            hoverPulse.stop();
            hoverPulse.setFromX(outerBevel.getScaleX());
            hoverPulse.setFromY(outerBevel.getScaleY());
            hoverPulse.setToX(1.0);
            hoverPulse.setToY(1.0);
            hoverPulse.play();
        });
        outerBevel.setOnMousePressed(e -> {
            applyPlayPressed();
            // Shift face down-right to look sunken
            playButton.setTranslateX(4);
            playButton.setTranslateY(4);
        });
        outerBevel.setOnMouseReleased(e -> {
            playButton.setTranslateX(2);
            playButton.setTranslateY(2);
            if (outerBevel.isHover())
                applyPlayHover();
            else
                applyPlayIdle();
        });
        outerBevel.setOnMouseClicked(e -> onPlayClicked());
        outerBevel.setStyle(
                "-fx-background-color: #7a5a18;" +
                        "-fx-background-radius: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);");

        playButtonContainer.getChildren().setAll(outerBevel);
    }

    private void applyPlayIdle() {
        playButton.setStyle(
                // Layered fill: bright highlight strip on top, base gold body
                "-fx-background-color:" +
                        "   linear-gradient(to bottom, #f5d080 0%, #f5d080 3px, #d4a035 3px, #c8922a 100%);" +
                        "-fx-background-insets: 0;" +
                        "-fx-background-radius: 0;" +
                        // Thin inner border: bright left/top edge, slightly darker right/bottom
                        "-fx-border-color: #fce89a #a07020 #a07020 #fce89a;" +
                        "-fx-border-width: 2px;" +
                        "-fx-cursor: hand;");
    }

    private void applyPlayHover() {
        playButton.setStyle(
                "-fx-background-color:" +
                        "   linear-gradient(to bottom, #ffe9a0 0%, #ffe9a0 3px, #f0c040 3px, #e8a828 100%);" +
                        "-fx-background-insets: 0;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: #fff5c0 #c09030 #c09030 #fff5c0;" +
                        "-fx-border-width: 2px;" +
                        "-fx-cursor: hand;" +
                        // Subtle warm tint only — no bloom
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,200,60,0.18), 6, 0.1, 0, 0);");
    }

    private void applyPlayPressed() {
        playButton.setStyle(
                // Sunken: flip highlight/shadow — darker gradient, shadow goes inward
                "-fx-background-color:" +
                        "   linear-gradient(to bottom, #b07820 0%, #b07820 3px, #c8922a 3px, #d4a035 100%);" +
                        "-fx-background-insets: 0;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: #7a5010 #fce89a #fce89a #7a5010;" +
                        "-fx-border-width: 2px;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.5), 0, 0.0, 0, 0);");
    }

    private void updateStatus(String text, String statusClass) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-success", "status-error", "status-neutral");
        statusLabel.getStyleClass().add(statusClass);
    }

    // Drawing Tool: Clears the row and draws all the save cards (either filled
    // or empty dashed ones)
    private void renderSlots() {
        int previousSelect = selectedSlot;
        profileSlotsBox.getChildren().clear();
        selectedSlot = -1;

        for (int i = 0; i < ProfileManager.getMaxSlots(); i++) {
            Wizard w = profiles.get(i);
            StackPane card = w == null ? buildEmptySlot(i) : buildProfileSlot(i, w);
            profileSlotsBox.getChildren().add(card);
        }

        if (previousSelect >= 0 && previousSelect < profiles.size() && profiles.get(previousSelect) != null) {
            selectedSlot = previousSelect;
            StackPane selected = (StackPane) profileSlotsBox.getChildren().get(selectedSlot);
            selected.setStyle(selectedCardStyle());
            updateStatus("Playing as " + profiles.get(selectedSlot).getName() + " — press Play!", "status-success");
        }
    }

    // Changes the skin of the wizard based on the direction (+1 for next, -1 for
    // previous)
    private void cycleSkin(int slot, Wizard wizard, int direction) {
        Wizard.WizardSkin[] skins = Wizard.WizardSkin.values();
        int currentIdx = -1;
        for (int i = 0; i < skins.length; i++) {
            if (skins[i] == wizard.getSkin()) {
                currentIdx = i;
                break;
            }
        }
        if (currentIdx != -1) {
            int nextIdx = (currentIdx + direction + skins.length) % skins.length;
            wizard.setSkin(skins[nextIdx]);
            ProfileManager.saveSlot(slot, wizard);
            renderSlots();
            SoundManager.playClick();
        }
    }

    // Builds a single wizard card for a specific slot
    private StackPane buildProfileSlot(int slot, Wizard wizard) {
        StackPane card = new StackPane();
        card.setPrefSize(239, 376);
        card.setMinSize(239, 376);
        card.setMaxSize(239, 376);
        card.setStyle(idleCardStyle());

        String classColor = "#506275";
        String classBg = "#1a1e29";
        String classGlow = "rgba(80, 98, 117, 0.5)";
        switch (wizard.getSkin()) {
            case ARCANE_WIZARD:
                classColor = "#a38aff";
                classBg = "#1a133d";
                classGlow = "rgba(163, 138, 255, 0.4)";
                break;
            case EMBER_MAGE:
                classColor = "#ff8a8a";
                classBg = "#3d1313";
                classGlow = "rgba(255, 80, 80, 0.4)";
                break;
            case PRISM_SAGE:
                classColor = "#ffe68a";
                classBg = "#3d3613";
                classGlow = "rgba(255, 210, 80, 0.4)";
                break;
            case GROVE_MAGUS:
                classColor = "#8aff8a";
                classBg = "#133d13";
                classGlow = "rgba(80, 255, 80, 0.4)";
                break;
        }

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 16 18 20 18;");

        // --- 1. NAME ---
        Label nameLabel = new Label(wizard.getName());
        nameLabel.getStyleClass().add("pixel-font");
        nameLabel.setStyle(
                "-fx-text-fill: #e2b96f;" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(one-pass-box, #000000, 0, 0.0, 2, 2);");

        // --- 2. PORTRAIT PEDESTAL ---
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(160, 160);
        iconPane.setMaxSize(160, 160);
        iconPane.setStyle(String.format(
                "-fx-border-color: %s;" +
                        "-fx-border-width: 3px;" +
                        "-fx-border-radius: 0;" +
                        "-fx-background-radius: 0;" +
                        "-fx-background-color: radial-gradient(center 50%% 50%%, radius 75%%, %s 0%%, #0d0f14 100%%);",
                classColor, classBg));

        StackPane glowPane = new StackPane(iconPane);
        glowPane.setStyle(String.format(
                "-fx-effect: dropshadow(three-pass-box, %s, 16, 0.5, 0, 0);", classGlow));

        ImageView playerView = new ImageView();
        java.net.URL imgUrl = ProfileController.class.getResource("/assets/" + wizard.getSkin().getImagePath());
        if (imgUrl != null) {
            Image img = new Image(imgUrl.toExternalForm());
            playerView.setImage(img);
            playerView.setFitWidth(130);
            playerView.setFitHeight(130);
            playerView.setPreserveRatio(true);
            playerView.setSmooth(false);
            playerView.setViewport(new Rectangle2D(0, 0, 128, 128));

            CustomSpriteTransition animation = new CustomSpriteTransition(
                    playerView, Duration.millis(800), 2, 2, 128.0, 128.0);
            animation.setCycleCount(Transition.INDEFINITE);
            animation.play();
            iconPane.getChildren().add(playerView);
        } else {
            Label emoji = new Label("🧙");
            emoji.setStyle("-fx-font-size: 46px;");
            iconPane.getChildren().add(emoji);
        }

        // --- LEFT / RIGHT ARROWS ---
        Button leftArrow = new Button("◀");
        leftArrow.getStyleClass().addAll("pixel-font", "arrow-button", "arrow-left");
        leftArrow.setOnMouseClicked(ev -> ev.consume());
        leftArrow.setOnAction(ev -> {
            ev.consume();
            cycleSkin(slot, wizard, -1);
        });

        Button rightArrow = new Button("▶");
        rightArrow.getStyleClass().addAll("pixel-font", "arrow-button", "arrow-right");
        rightArrow.setOnMouseClicked(ev -> ev.consume());
        rightArrow.setOnAction(ev -> {
            ev.consume();
            cycleSkin(slot, wizard, 1);
        });

        HBox portraitContainer = new HBox(4);
        portraitContainer.setAlignment(Pos.CENTER);
        portraitContainer.getChildren().addAll(leftArrow, glowPane, rightArrow);

        // --- 3. CLASS RIBBON ---
        Label skinLabel = new Label("✦  " + wizard.getSkin().getDisplayName().toUpperCase() + "  ✦");
        skinLabel.getStyleClass().add("pixel-font");
        skinLabel.setStyle(String.format(
                "-fx-text-fill: %s;" +
                        "-fx-background-color: %s;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 14;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: %s;" +
                        "-fx-border-width: 1px;",
                classColor, classBg, classColor));

        // --- 4. STATS DIVIDER ---
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setPrefWidth(150);
        divider.setStyle("-fx-background-color: #2a3040;");

        // --- 5. WIN / LOSS BADGES ---
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER);

        VBox winBox = new VBox(2);
        winBox.setAlignment(Pos.CENTER);
        Label winNum = new Label(String.valueOf(wizard.getWins()));
        winNum.getStyleClass().add("pixel-font");
        winNum.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 32px; -fx-font-weight: bold;");
        Label winTxt = new Label("WINS");
        winTxt.getStyleClass().add("pixel-font");
        winTxt.setStyle("-fx-text-fill: #3a6e40; -fx-font-size: 11px;");
        winBox.getChildren().addAll(winNum, winTxt);

        Label sep = new Label("|");
        sep.setStyle("-fx-text-fill: #2a3040; -fx-font-size: 20px;");

        VBox lossBox = new VBox(2);
        lossBox.setAlignment(Pos.CENTER);
        Label lossNum = new Label(String.valueOf(wizard.getLosses()));
        lossNum.getStyleClass().add("pixel-font");
        lossNum.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 32px; -fx-font-weight: bold;");
        Label lossTxt = new Label("LOSSES");
        lossTxt.getStyleClass().add("pixel-font");
        lossTxt.setStyle("-fx-text-fill: #6e3a3a; -fx-font-size: 11px;");
        lossBox.getChildren().addAll(lossNum, lossTxt);

        statsBox.getChildren().addAll(winBox, sep, lossBox);

        // --- DELETE BUTTON ---
        Button deleteBtn = new Button("✕");
        deleteBtn.getStyleClass().add("pixel-font");
        deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #555577; -fx-font-size: 16px;" +
                        "-fx-padding: 6 10; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> onDeleteSlot(slot));
        StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);

        content.getChildren().addAll(nameLabel, portraitContainer, skinLabel, divider, statsBox);
        card.getChildren().addAll(content, deleteBtn);

        card.setOnMouseClicked(e -> selectSlot(slot, card));
        card.setOnMouseEntered(e -> {
            card.setTranslateY(-8);
            if (selectedSlot != slot)
                card.setStyle(hoverCardStyle());
        });
        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            if (selectedSlot != slot)
                card.setStyle(idleCardStyle());
        });

        return card;
    }

    // Builds an empty slot card for a specific slot
    private StackPane buildEmptySlot(int slot) {
        StackPane card = new StackPane();
        card.setPrefSize(239, 376);
        card.setMinSize(239, 376);
        card.setMaxSize(239, 376);
        card.setStyle(emptyCardStyle());

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);

        Label plus = new Label("+");
        plus.getStyleClass().add("pixel-font");
        plus.setStyle("-fx-font-size: 72px; -fx-text-fill: #333355;");

        Label hint = new Label("New Wizard");
        hint.getStyleClass().add("pixel-font");
        hint.setStyle("-fx-font-size: 16px; -fx-text-fill: #3a3a5a;");

        content.getChildren().addAll(plus, hint);
        card.getChildren().add(content);

        card.setOnMouseClicked(e -> onCreateProfile(slot));
        card.setOnMouseEntered(e -> {
            card.setTranslateY(-8);
            card.setStyle(emptyHoverCardStyle());
        });
        card.setOnMouseExited(e -> {
            card.setTranslateY(0);
            card.setStyle(emptyCardStyle());
        });

        return card;
    }

    // Selects a slot and highlights it
    private void selectSlot(int slot, StackPane card) {
        if (selectedSlot >= 0 && selectedSlot < profileSlotsBox.getChildren().size()) {
            StackPane previous = (StackPane) profileSlotsBox.getChildren().get(selectedSlot);
            previous.setStyle(idleCardStyle());
        }
        selectedSlot = slot;
        card.setStyle(selectedCardStyle());
        updateStatus("Playing as " + profiles.get(slot).getName() + " — press Play!", "status-success");
    }

    // Play Action
    private void onPlayClicked() {
        if (selectedSlot < 0 || profiles.get(selectedSlot) == null) {
            updateStatus("Select a wizard first.", "status-error");
            return;
        }
        Wizard w = profiles.get(selectedSlot);
        SessionManager.setMyName(w.getName());
        SessionManager.setActiveProfile(selectedSlot, w);
        Main.navigateWithSlideLeftTransition("menu-view.fxml");
    }

    // Creator Action
    // ─── REPLACE onCreateProfile with this ────────────────────────────────────

    private void onCreateProfile(int slot) {
        Stage dialog = buildDialogStage("New Wizard", 380, 310);

        // Input field
        javafx.scene.control.TextField nameField = new javafx.scene.control.TextField();
        nameField.setPromptText("Enter name...");

        nameField.setStyle(
                "-fx-background-color: #0b0c10;" +
                        "-fx-border-color: #4a5a70;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-text-fill: #e2b96f;" +
                        "-fx-prompt-text-fill: #506275;" +
                        "-fx-font-family: 'Pixelify Sans';" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8 12;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-radius: 0;");
        // Enforce 16 char max
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 16)
                nameField.setText(oldVal);
        });

        Label hint = new Label("Max 16 characters");
        hint.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 11px; -fx-text-fill: #506275;");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 11px; -fx-text-fill: #ff6b6b;");
        errorLabel.setMinHeight(16);

        // Buttons
        StackPane cancelBtn = buildDialogButton("Cancel", false);
        StackPane confirmBtn = buildDialogButton("✦  Create", true);

        HBox btnRow = new HBox(16, cancelBtn, confirmBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(10);
        body.setStyle("-fx-padding: 24 24 20 24;");
        body.getChildren().addAll(
                styledDialogLabel("Choose your wizard's name:"),
                nameField,
                hint,
                errorLabel,
                btnRow);

        dialog.getScene().setRoot(buildDialogRoot("✦  New Wizard  ✦", false, body, dialog));

        cancelBtn.setOnMouseClicked(e -> dialog.close());
        confirmBtn.setOnMouseClicked(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                errorLabel.setText("Name can't be empty.");
                return;
            }
            Wizard w = new Wizard(name, 200, Wizard.WizardSkin.ARCANE_WIZARD);
            profiles.set(slot, w);
            ProfileManager.saveSlot(slot, w);
            dialog.close();
            renderSlots();
            updateStatus("Wizard \"" + name + "\" created!", "status-success");
        });

        // Allow Enter key to confirm
        nameField.setOnAction(e -> confirmBtn.getOnMouseClicked().handle(
                new javafx.scene.input.MouseEvent(
                        javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                        javafx.scene.input.MouseButton.PRIMARY, 1,
                        false, false, false, false, true, false, false, false, false, false, null)));

        dialog.showAndWait();
    }

    // ─── REPLACE onDeleteSlot with this ───────────────────────────────────────

    private void onDeleteSlot(int slot) {
        String wizardName = profiles.get(slot).getName();
        Stage dialog = buildDialogStage("Delete Wizard", 380, 280);

        // Warning box
        VBox warningBox = new VBox(6);
        warningBox.setStyle(
                "-fx-background-color: #1f0a0a;" +
                        "-fx-border-color: #cc3333;" +
                        "-fx-border-width: 1px;" +
                        "-fx-padding: 14 18;");
        Label warnTitle = new Label("Delete \"" + wizardName + "\"?");
        warnTitle.setStyle(
                "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 15px; -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
        Label warnBody = new Label("This will erase all progress.");
        warnBody.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 12px; -fx-text-fill: #994444;");
        warningBox.getChildren().addAll(warnTitle, warnBody);

        Label subNote = new Label("This cannot be undone.");
        subNote.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 11px; -fx-text-fill: #506275;");

        StackPane cancelBtn = buildDialogButton("Cancel", false);
        StackPane deleteBtn = buildDialogButton("✕  Delete", true, true);

        HBox btnRow = new HBox(16, cancelBtn, deleteBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(14);
        body.setStyle("-fx-padding: 20 24 20 24;");
        body.getChildren().addAll(warningBox, subNote, btnRow);

        dialog.getScene().setRoot(buildDialogRoot("⚠  Delete Wizard  ⚠", true, body, dialog));

        cancelBtn.setOnMouseClicked(e -> dialog.close());
        deleteBtn.setOnMouseClicked(e -> {
            ProfileManager.deleteSlot(slot);
            profiles.set(slot, null);
            if (selectedSlot == slot)
                selectedSlot = -1;
            dialog.close();
            renderSlots();
            updateStatus("Wizard deleted.", "status-neutral");
        });

        dialog.showAndWait();
    }

    // ─── SHARED DIALOG HELPERS ─────────────────────────────────────────────────

    private Stage buildDialogStage(String title, double width, double height) {
        Stage stage = new Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.setTitle(title);
        stage.setResizable(false);

        // Transparent scene so our custom border shows cleanly
        javafx.scene.layout.StackPane placeholder = new javafx.scene.layout.StackPane();
        placeholder.setPrefSize(width, height);
        javafx.scene.Scene scene = new javafx.scene.Scene(placeholder, width, height);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();
        return stage;
    }

    // Assembles the full dialog chrome: gold/red bevel frame + title bar + body
    private VBox buildDialogRoot(String titleText, boolean danger, VBox body, Stage stage) {
        // Title bar
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

        // Allow dragging the dialog by its title bar
        final double[] dragDelta = new double[2];
        titleBar.setOnMousePressed(e -> {
            dragDelta[0] = stage.getX() - e.getScreenX();
            dragDelta[1] = stage.getY() - e.getScreenY();
        });
        titleBar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + dragDelta[0]);
            stage.setY(e.getScreenY() + dragDelta[1]);
        });

        VBox inner = new VBox(0, titleBar, body);
        inner.setStyle(
                "-fx-background-color: #1a1e2e;" +
                        "-fx-border-color: " + (danger ? "#cc3333" : "#e8c97a") + ";" +
                        "-fx-border-width: 2px;");

        // Pixel corner notches
        javafx.scene.shape.Rectangle notchTL = dialogNotch(0, 0);
        javafx.scene.shape.Rectangle notchTR = dialogNotch(-4, 0);
        javafx.scene.shape.Rectangle notchBL = dialogNotch(0, -4);
        javafx.scene.shape.Rectangle notchBR = dialogNotch(-4, -4);
        StackPane.setAlignment(notchTR, Pos.TOP_RIGHT);
        StackPane.setAlignment(notchBL, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(notchBR, Pos.BOTTOM_RIGHT);

        StackPane root = new StackPane(inner, notchTL, notchTR, notchBL, notchBR);
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

    // Gold confirm button (danger=false) or red delete button (danger=true)
    private StackPane buildDialogButton(String text, boolean isPrimary) {
        return buildDialogButton(text, isPrimary, false);
    }

    private StackPane buildDialogButton(String text, boolean isPrimary, boolean danger) {
        // Outer bevel container
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

        // Set up the specific styles for the inner button face
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
            // Cancel / Normal grey button
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

        // Inner face (126x32, sits 2px inset)
        StackPane inner = new StackPane(label);
        inner.setPrefSize(126, 32);
        inner.setMinSize(126, 32);
        inner.setMaxSize(126, 32);

        // Define dynamic styles
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

        // Mouse behavior matching the play button
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

    private void styleDialog(Dialog<?> dialog) {
        dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e;");
        dialog.getDialogPane().lookupAll(".label").forEach(node -> node.setStyle("-fx-font-family: 'Georgia';"));
    }

    private String idleCardStyle() {
        return "-fx-background-color: #12151e;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #4a5a70;" +
                "-fx-border-width: 4;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);" +
                "-fx-cursor: hand;";
    }

    private String hoverCardStyle() {
        return "-fx-background-color: #12151e;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #e2b96f;" +
                "-fx-border-width: 4;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);" +
                "-fx-cursor: hand;";
    }

    private String selectedCardStyle() {
        return "-fx-background-color: #12151e;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #e2b96f;" +
                "-fx-border-width: 4;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(one-pass-box, #e2b96f, 0, 0.0, 6, 6);" +
                "-fx-cursor: hand;";
    }

    private String emptyCardStyle() {
        return "-fx-background-color: #0b0c10;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #22252e;" +
                "-fx-border-width: 4;" +
                "-fx-border-style: dashed;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.4), 0, 0.0, 6, 6);" +
                "-fx-cursor: hand;";
    }

    private String emptyHoverCardStyle() {
        return "-fx-background-color: #12151e;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #4a5a70;" +
                "-fx-border-width: 4;" +
                "-fx-border-style: dashed;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);" +
                "-fx-cursor: hand;";
    }
}