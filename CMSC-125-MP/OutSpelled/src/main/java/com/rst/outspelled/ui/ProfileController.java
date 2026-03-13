package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.network.SessionManager;
import com.rst.outspelled.util.ProfileManager;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Optional;

public class ProfileController {

    @FXML private HBox profileSlotsBox;
    @FXML private Label statusLabel;

    // which slot is currently selected (-1 = none)
    private int selectedSlot = -1;
    private List<Wizard> profiles;

    @FXML
    public void initialize() {
        profiles = ProfileManager.loadAll();
        renderSlots();
    }

    private void renderSlots() {
        profileSlotsBox.getChildren().clear();
        selectedSlot = -1;

        for (int i = 0; i < ProfileManager.getMaxSlots(); i++) {
            Wizard w = profiles.get(i);
            StackPane card = w == null ? buildEmptySlot(i) : buildProfileSlot(i, w);
            profileSlotsBox.getChildren().add(card);
        }
    }

    private StackPane buildProfileSlot(int slot, Wizard wizard) {
        StackPane card = new StackPane();
        card.setPrefSize(180, 220);
        card.setStyle(idleCardStyle());

        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 16;");

        // Wizard icon placeholder
        Rectangle icon = new Rectangle(80, 90);
        icon.setArcWidth(12); icon.setArcHeight(12);
        icon.setFill(Color.web("#2a1a4a"));
        icon.setStroke(Color.web("#e2b96f"));
        icon.setStrokeWidth(2);

        Label emoji = new Label("🧙");
        emoji.setStyle("-fx-font-size: 36px;");

        StackPane iconPane = new StackPane(icon, emoji);

        Label nameLabel = new Label(wizard.getName());
        nameLabel.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 15px;" +
                " -fx-font-weight: bold; -fx-font-family: 'Georgia';");

        Label recordLabel = new Label(wizard.getWinLossRecord());
        recordLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 12px;");

        Label skinLabel = new Label(wizard.getSkin().getDisplayName());
        skinLabel.setStyle("-fx-text-fill: #666688; -fx-font-size: 11px;");

        // Delete button (small, top-right)
        Button deleteBtn = new Button("✕");
        deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #666688; -fx-font-size: 11px;" +
                        "-fx-padding: 2 6; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> onDeleteSlot(slot));
        StackPane.setAlignment(deleteBtn, Pos.TOP_RIGHT);

        content.getChildren().addAll(iconPane, nameLabel, recordLabel, skinLabel);
        card.getChildren().addAll(content, deleteBtn);

        card.setOnMouseClicked(e -> selectSlot(slot, card));
        card.setOnMouseEntered(e -> {
            if (selectedSlot != slot)
                card.setStyle(hoverCardStyle());
        });
        card.setOnMouseExited(e -> {
            if (selectedSlot != slot)
                card.setStyle(idleCardStyle());
        });

        return card;
    }

    private StackPane buildEmptySlot(int slot) {
        StackPane card = new StackPane();
        card.setPrefSize(180, 220);
        card.setStyle(emptyCardStyle());

        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);

        Label plus = new Label("+");
        plus.setStyle("-fx-font-size: 48px; -fx-text-fill: #444466;");

        Label hint = new Label("New Wizard");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #444466;");

        content.getChildren().addAll(plus, hint);
        card.getChildren().add(content);

        card.setOnMouseClicked(e -> onCreateProfile(slot));
        card.setOnMouseEntered(e ->
                card.setStyle(emptyHoverCardStyle()));
        card.setOnMouseExited(e ->
                card.setStyle(emptyCardStyle()));

        return card;
    }

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

    private void onCreateProfile(int slot) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Wizard");
        dialog.setHeaderText("Choose your wizard's name");
        dialog.setContentText("Name:");
        styleDialog(dialog);

        Optional<String> result = dialog.showAndWait();
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
            Wizard w = new Wizard(name, 200,
                    Wizard.WizardSkin.EMBER_MAGE,
                    Wizard.ArenaBackground.DARK_TOWER);
            profiles.set(slot, w);
            ProfileManager.saveSlot(slot, w);
            renderSlots();
            statusLabel.setText("Wizard \"" + name + "\" created!");
            statusLabel.setStyle("-fx-text-fill: #4caf50;");
        });
    }

    private void onDeleteSlot(int slot) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Wizard");
        confirm.setHeaderText("Delete " + profiles.get(slot).getName() + "?");
        confirm.setContentText("This will erase all progress. This cannot be undone.");
        styleDialog(confirm);

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

    private void styleDialog(Dialog<?> dialog) {
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #1a1a2e;" +
                        "-fx-font-family: 'Georgia';");
    }

    private String idleCardStyle() {
        return "-fx-background-color: #12122a;" +
                "-fx-border-color: #2a2a4a;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;";
    }
    private String hoverCardStyle() {
        return "-fx-background-color: #1a1a3a;" +
                "-fx-border-color: #e2b96f;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;";
    }
    private String selectedCardStyle() {
        return "-fx-background-color: #1a1a3a;" +
                "-fx-border-color: #e2b96f;" +
                "-fx-border-width: 3;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, #e2b96f, 12, 0.4, 0, 0);" +
                "-fx-cursor: hand;";
    }
    private String emptyCardStyle() {
        return "-fx-background-color: #0d0d1a;" +
                "-fx-border-color: #222244;" +
                "-fx-border-width: 2;" +
                "-fx-border-style: dashed;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;";
    }
    private String emptyHoverCardStyle() {
        return "-fx-background-color: #12122a;" +
                "-fx-border-color: #444466;" +
                "-fx-border-width: 2;" +
                "-fx-border-style: dashed;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;";
    }
}