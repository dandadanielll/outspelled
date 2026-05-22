package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.network.SessionManager;
import com.rst.outspelled.util.ProfileManager;
import com.rst.outspelled.util.SoundManager;
import javafx.animation.Transition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;

public class ProfileController {

    @FXML
    private HBox profileSlotsBox;
    @FXML
    private Label statusLabel;

    // which slot is currently selected (-1 = none)
    private int selectedSlot = -1;
    private List<Wizard> profiles;

    @FXML
    public void initialize() {
        profiles = ProfileManager.loadAll();
        renderSlots();
    }

    // Drawing Tool: Clears the row and draws all the save cards (either filled
    // or empty dashed ones)
    private void renderSlots() {
        int previousSelect = selectedSlot;
        profileSlotsBox.getChildren().clear();
        selectedSlot = -1;

        // Loop through all possible save slots and build a card for each
        for (int i = 0; i < ProfileManager.getMaxSlots(); i++) {
            Wizard w = profiles.get(i);
            // If slot is empty, make a "+" card. If it has a wizard, make a character card.
            StackPane card = w == null ? buildEmptySlot(i) : buildProfileSlot(i, w);
            profileSlotsBox.getChildren().add(card);
        }

        // This ensures that when you return to the profile screen, the previously
        // selected
        // profile is still highlighted
        if (previousSelect >= 0 && previousSelect < profiles.size() && profiles.get(previousSelect) != null) {
            selectedSlot = previousSelect;
            StackPane selected = (StackPane) profileSlotsBox.getChildren().get(selectedSlot);
            selected.setStyle(selectedCardStyle());
            statusLabel.setText("Playing as " + profiles.get(selectedSlot).getName()
                    + " — press Play!");
            statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 13px;");
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
        card.setPrefSize(210, 330);
        card.setMinSize(210, 330);
        card.setMaxSize(210, 330);
        card.setStyle(idleCardStyle());

        // Dynamic theme colors based on Wizard Skin for premium in-game card feel
        String classColor = "#506275";
        String classBg = "#1a1e29";
        String classGlow = "rgba(80, 98, 117, 0.5)";
        // Sets the color of the card based on the wizard's class
        switch (wizard.getSkin()) {
            case ARCANE_WIZARD:
                classColor = "#a38aff"; // Arcane purple
                classBg = "#1a133d";
                classGlow = "rgba(163, 138, 255, 0.4)";
                break;
            case EMBER_MAGE:
                classColor = "#ff8a8a"; // Fire red
                classBg = "#3d1313";
                classGlow = "rgba(255, 80, 80, 0.4)";
                break;
            case PRISM_SAGE:
                classColor = "#ffe68a"; // Prism yellow
                classBg = "#3d3613";
                classGlow = "rgba(255, 210, 80, 0.4)";
                break;
            case GROVE_MAGUS:
                classColor = "#8aff8a"; // Grove green
                classBg = "#133d13";
                classGlow = "rgba(80, 255, 80, 0.4)";
                break;
        }

        // Main container for the card
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 16 14;");

        // --- 1. NAME ---
        Label nameLabel = new Label(wizard.getName());
        nameLabel.getStyleClass().add("pixel-font");
        nameLabel.setStyle(
                "-fx-text-fill: #e2b96f; " +
                        "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-effect: dropshadow(one-pass-box, #000000, 0, 0.0, 2, 2);");

        // --- 2. PORTRAIT PEDESTAL WITH RADIAL SHINE + CLASS GLOW BORDER ---
        StackPane iconPane = new StackPane();
        iconPane.setPrefSize(110, 110);
        iconPane.setMaxSize(110, 110);
        String iconPaneStyle = String.format(
                "-fx-border-color: %s; " +
                        "-fx-border-width: 3px; " +
                        "-fx-border-radius: 0; " +
                        "-fx-background-radius: 0; " +
                        "-fx-background-color: radial-gradient(center 50%% 50%%, radius 75%%, %s 0%%, #0d0f14 100%%);",
                classColor, classBg);
        iconPane.setStyle(iconPaneStyle);

        // Outer glow wrapper around portrait
        StackPane glowPane = new StackPane(iconPane);
        glowPane.setStyle(String.format(
                "-fx-effect: dropshadow(three-pass-box, %s, 16, 0.5, 0, 0);",
                classGlow));

        // Wizard sprite loaded from skin's imagePath
        ImageView playerView = new ImageView();
        java.net.URL imgUrl = ProfileController.class.getResource("/assets/" + wizard.getSkin().getImagePath());
        if (imgUrl != null) {
            Image img = new Image(imgUrl.toExternalForm());
            playerView.setImage(img);
            playerView.setFitWidth(90);
            playerView.setFitHeight(90);
            playerView.setPreserveRatio(true);
            playerView.setSmooth(false);
            playerView.setViewport(new Rectangle2D(0, 0, 128, 128));

            int totalFrames = 2;
            int columns = 2;
            double FRAME_WIDTH = 128.0;
            double FRAME_HEIGHT = 128.0;
            CustomSpriteTransition animation = new CustomSpriteTransition(
                    playerView, Duration.millis(800), totalFrames, columns, FRAME_WIDTH, FRAME_HEIGHT);
            animation.setCycleCount(Transition.INDEFINITE);
            animation.play();
        }

        // Fallback
        if (imgUrl != null) {
            iconPane.getChildren().add(playerView);
        } else {
            Label emoji = new Label("🧙");
            emoji.setStyle("-fx-font-size: 46px;");
            iconPane.getChildren().add(emoji);
        }

        // Clickable left/right arrows around the pedestal box
        Button leftArrow = new Button("◀");
        leftArrow.getStyleClass().add("pixel-font");
        leftArrow.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #e2b96f; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 0 6 0 0;");
        leftArrow.setOnMouseEntered(ev -> leftArrow.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 6 0 0;"));
        leftArrow.setOnMouseExited(ev -> leftArrow.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #e2b96f; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 6 0 0;"));
        leftArrow.setOnMouseClicked(ev -> ev.consume());
        leftArrow.setOnAction(ev -> {
            ev.consume();
            cycleSkin(slot, wizard, -1);
        });

        Button rightArrow = new Button("▶");
        rightArrow.getStyleClass().add("pixel-font");
        rightArrow.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #e2b96f; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 0 0 0 6;");
        rightArrow.setOnMouseEntered(ev -> rightArrow.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 0 0 6;"));
        rightArrow.setOnMouseExited(ev -> rightArrow.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #e2b96f; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 0 0 6;"));
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
        String skinLabelStyle = String.format(
                "-fx-text-fill: %s; " +
                        "-fx-background-color: %s; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 3 14; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 1px;",
                classColor, classBg, classColor);
        skinLabel.setStyle(skinLabelStyle);

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
        winNum.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label winTxt = new Label("WINS");
        winTxt.getStyleClass().add("pixel-font");
        winTxt.setStyle("-fx-text-fill: #3a6e40; -fx-font-size: 9px;");
        winBox.getChildren().addAll(winNum, winTxt);

        Label sep = new Label("|");
        sep.setStyle("-fx-text-fill: #2a3040; -fx-font-size: 20px;");

        VBox lossBox = new VBox(2);
        lossBox.setAlignment(Pos.CENTER);
        Label lossNum = new Label(String.valueOf(wizard.getLosses()));
        lossNum.getStyleClass().add("pixel-font");
        lossNum.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label lossTxt = new Label("LOSSES");
        lossTxt.getStyleClass().add("pixel-font");
        lossTxt.setStyle("-fx-text-fill: #6e3a3a; -fx-font-size: 9px;");
        lossBox.getChildren().addAll(lossNum, lossTxt);

        statsBox.getChildren().addAll(winBox, sep, lossBox);

        // Delete button
        Button deleteBtn = new Button("✕");
        deleteBtn.getStyleClass().add("pixel-font");
        deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #555577; -fx-font-size: 12px;" +
                        "-fx-padding: 4 8; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> onDeleteSlot(slot));
        StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);

        // Pack all the components together
        content.getChildren().addAll(nameLabel, portraitContainer, skinLabel, divider, statsBox);
        card.getChildren().addAll(content, deleteBtn);

        // Add mouse event listeners
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
        card.setPrefSize(210, 330);
        card.setMinSize(210, 330);
        card.setMaxSize(210, 330);
        card.setStyle(emptyCardStyle());

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);

        Label plus = new Label("+");
        plus.getStyleClass().add("pixel-font");
        plus.setStyle("-fx-font-size: 56px; -fx-text-fill: #333355;");

        Label hint = new Label("New Wizard");
        hint.getStyleClass().add("pixel-font");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #3a3a5a;");

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
        // deselect previous
        renderSlots();
        // re-find the card and highlight it
        selectedSlot = slot;
        StackPane selected = (StackPane) profileSlotsBox.getChildren().get(slot);
        selected.setStyle(selectedCardStyle());
        statusLabel.setText("Playing as " + profiles.get(slot).getName()
                + " — press Play!");
        statusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 13px;");
    }

    // Play Action: Verifies selection, logs profile into the global session, and
    // opens main menu
    @FXML
    private void onPlayClicked() {
        if (selectedSlot < 0 || profiles.get(selectedSlot) == null) {
            statusLabel.setText("Select a wizard first.");
            statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px;");
            return;
        }
        Wizard w = profiles.get(selectedSlot);
        SessionManager.setMyName(w.getName());
        SessionManager.setActiveProfile(selectedSlot, w);
        Main.navigateTo("menu-view.fxml");
    }

    // Creator Action: Pops up a text prompt box to name and create a brand-new
    // Wizard save
    private void onCreateProfile(int slot) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Wizard");
        dialog.setHeaderText("Choose your wizard's name");
        dialog.setContentText("Name:");
        styleDialog(dialog);

        // Show the dialog and wait for user input
        Optional<String> result = dialog.showAndWait();
        // Process the user's input
        result.ifPresent(name -> {
            name = name.trim();
            if (name.isEmpty()) {
                statusLabel.setText("Name can't be empty.");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                return;
            }
            if (name.length() > 16) {
                statusLabel.setText("Name too long (max 16 chars).");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                return;
            }
            Wizard w = new Wizard(name, 200, Wizard.WizardSkin.ARCANE_WIZARD);
            profiles.set(slot, w);
            ProfileManager.saveSlot(slot, w);
            renderSlots();
            statusLabel.setText("Wizard \"" + name + "\" created!");
            statusLabel.setStyle("-fx-text-fill: #4caf50;");
        });
    }

    // Deletes a wizard from the specified slot
    private void onDeleteSlot(int slot) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Wizard");
        confirm.setHeaderText("Delete " + profiles.get(slot).getName() + "?");
        confirm.setContentText("This will erase all progress. This cannot be undone.");
        styleDialog(confirm);

        // Show the confirmation dialog and wait for user input
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                ProfileManager.deleteSlot(slot);
                profiles.set(slot, null);
                renderSlots();
                statusLabel.setText("Wizard deleted.");
                statusLabel.setStyle("-fx-text-fill: #a0a0c0;");
            }
        });
    }

    // Styles the dialog box to match the game's theme
    private void styleDialog(Dialog<?> dialog) {
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #1a1a2e;" +
                        "-fx-font-family: 'Georgia';");
    }

    // Returns the idle style for a wizard card
    private String idleCardStyle() {
        return "-fx-background-color: #12151e;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #4a5a70;" +
                "-fx-border-width: 4;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);" +
                "-fx-cursor: hand;";
    }

    // Returns the hover style for a wizard card
    private String hoverCardStyle() {
        return "-fx-background-color: #12151e;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #e2b96f;" +
                "-fx-border-width: 4;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);" +
                "-fx-cursor: hand;";
    }

    // Returns the selected style for a wizard card
    private String selectedCardStyle() {
        return "-fx-background-color: #12151e;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #e2b96f;" +
                "-fx-border-width: 4;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(one-pass-box, #e2b96f, 0, 0.0, 6, 6);" +
                "-fx-cursor: hand;";
    }

    // Returns the empty style for a wizard card
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

    // Returns the empty hover style for a wizard card
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