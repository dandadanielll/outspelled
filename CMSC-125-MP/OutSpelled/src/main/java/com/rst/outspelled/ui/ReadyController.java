package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.network.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Ready-check screen: both players must confirm before the mana wheel starts.
 * In local play both click Ready on this machine; in LAN each client sends READY to server.
 */
public class ReadyController {

    static ReadyController instance;

    @FXML private Label player1NameLabel;
    @FXML private Label player2NameLabel;
    @FXML private Button player1ReadyButton;
    @FXML private Button player2ReadyButton;
    @FXML private Label player1StatusLabel;
    @FXML private Label player2StatusLabel;
    @FXML private Label statusLabel;

    private static Wizard wizard1;
    private static Wizard wizard2;
    private static boolean isNetworkMode = false;

    private boolean player1Ready = false;
    private boolean player2Ready = false;

    public static void setWizards(Wizard w1, Wizard w2) {
        wizard1 = w1;
        wizard2 = w2;
    }

    public static void setNetworkMode(boolean network) {
        isNetworkMode = network;
    }

    @FXML
    public void initialize() {
        instance = this;
        if (isNetworkMode) {
            player1NameLabel.setText(SessionManager.getPlayer1Name().isEmpty() ? "Player 1..." : SessionManager.getPlayer1Name());
            player2NameLabel.setText(SessionManager.getPlayer2Name().isEmpty() ? "Player 2..." : SessionManager.getPlayer2Name());
            player1ReadyButton.setVisible(SessionManager.getMyPlayerId() == 1);
            player2ReadyButton.setVisible(SessionManager.getMyPlayerId() == 2);
            statusLabel.setText("Click Ready when you're set. Waiting for both players...");
        } else {
            if (wizard1 != null) player1NameLabel.setText(wizard1.getName());
            if (wizard2 != null) player2NameLabel.setText(wizard2.getName());
            statusLabel.setText("Click Ready when you're set. (Local: one machine, two buttons.)");
        }
        player1StatusLabel.setText("");
        player2StatusLabel.setText("");
    }

    @FXML
    private void onPlayer1ReadyClicked() {
        if (player1Ready) return;
        player1Ready = true;
        player1StatusLabel.setText("✓ Ready");
        player1ReadyButton.setDisable(true);
        player1ReadyButton.setText("Ready ✓");
        if (isNetworkMode && SessionManager.getClient() != null) {
            SessionManager.getClient().sendReady();
        }
        checkBothReady();
    }

    @FXML
    private void onPlayer2ReadyClicked() {
        if (player2Ready) return;
        player2Ready = true;
        player2StatusLabel.setText("✓ Ready");
        player2ReadyButton.setDisable(true);
        player2ReadyButton.setText("Ready ✓");
        if (isNetworkMode && SessionManager.getClient() != null) {
            SessionManager.getClient().sendReady();
        }
        checkBothReady();
    }

    /**
     * Called from network layer when we receive the other player's name (so joiner sees host name).
     */
    public static void updatePlayerName(int playerId, String name) {
        Platform.runLater(() -> {
            if (instance != null && name != null && !name.isEmpty()) {
                if (playerId == 1) instance.player1NameLabel.setText(name);
                else instance.player2NameLabel.setText(name);
            }
        });
    }

    /**
     * Called from network layer when the other player's ready state is received.
     */
    public void setPlayer1Ready(boolean ready) {
        Platform.runLater(() -> {
            player1Ready = ready;
            if (ready) {
                player1StatusLabel.setText("✓ Ready");
                player1ReadyButton.setDisable(true);
                player1ReadyButton.setText("Ready ✓");
            }
            checkBothReady();
        });
    }

    public void setPlayer2Ready(boolean ready) {
        Platform.runLater(() -> {
            player2Ready = ready;
            if (ready) {
                player2StatusLabel.setText("✓ Ready");
                player2ReadyButton.setDisable(true);
                player2ReadyButton.setText("Ready ✓");
            }
            checkBothReady();
        });
    }

    private void checkBothReady() {
        if (!player1Ready || !player2Ready) return;
        statusLabel.setText("Both ready! Starting mana wheel...");
        statusLabel.setStyle("-fx-text-fill: #4caf50;");
        if (isNetworkMode) {
            // Navigation to wheel is done by client listener on BOTH_READY
            return;
        }
        new Thread(() -> {
            try {
                Thread.sleep(800);
                Platform.runLater(() -> {
                    WheelController.setWizards(wizard1, wizard2);
                    WheelController.setNetworkMode(false);
                    Main.navigateTo("wheel-view.fxml");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
