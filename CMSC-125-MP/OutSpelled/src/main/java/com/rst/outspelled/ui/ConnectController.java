package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.model.Wizard.ArenaBackground;
import com.rst.outspelled.model.Wizard.WizardSkin;
import com.rst.outspelled.network.GameClient;
import com.rst.outspelled.network.GameServer;
import com.rst.outspelled.network.Protocol;
import com.rst.outspelled.network.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ConnectController {

    @FXML private TextField hostField;
    @FXML private Label statusLabel;
    @FXML private Label localIpLabel;

    private static GameServer server;

    @FXML
    public void initialize() {
        statusLabel.setText("");
        displayLocalIp();
    }

    @FXML
    private void onHostClicked() {
        String name = "Player"; // Placeholder name
        statusLabel.setText("Starting server...");
        statusLabel.setStyle("-fx-text-fill: #a0a0c0;");
        new Thread(() -> {
            try {
                if (server != null) server.stop();
                server = new GameServer(Protocol.DEFAULT_PORT);
                server.start();
                Thread.sleep(500);
                Platform.runLater(() -> connectAsClient("127.0.0.1", name));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Server failed: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                });
            }
        }).start();
    }

    @FXML
    private void onJoinClicked() {
        String name = "Player"; // Placeholder name
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            statusLabel.setText("Enter server IP to join.");
            return;
        }
        statusLabel.setText("Connecting...");
        statusLabel.setStyle("-fx-text-fill: #a0a0c0;");
        connectAsClient(host, name);
    }

    private void connectAsClient(String host, String name) {
        GameClient client = new GameClient(host, Protocol.DEFAULT_PORT);
        SessionManager.setClient(client);
        SessionManager.setMyName(name);

        client.setListener(new GameClient.Listener() {
            @Override
            public void onJoinOk(int myPlayerId) {
                Platform.runLater(() -> {
                    SessionManager.setMyPlayerId(myPlayerId);
                    SessionManager.setPlayer1Name(myPlayerId == 1 ? name : SessionManager.getPlayer1Name());
                    SessionManager.setPlayer2Name(myPlayerId == 2 ? name : SessionManager.getPlayer2Name());
                    statusLabel.setText("");
                    ReadyController.setWizards(null, null);
                    ReadyController.setNetworkMode(true);
                    Main.navigateTo("ready-view.fxml");
                });
            }

            @Override
            public void onPlayerJoined(int playerId, String playerName) {
                Platform.runLater(() -> {
                    if (playerId == 1) SessionManager.setPlayer1Name(playerName);
                    else SessionManager.setPlayer2Name(playerName);
                    ReadyController.updatePlayerName(playerId, playerName);
                    if (ReadyController.instance != null) {
                        ReadyController.instance.initialize();
                    }
                });
            }

            @Override
            public void onReadyAck(int playerId) {
                Platform.runLater(() -> {
                    if (ReadyController.instance != null) {
                        if (playerId == 1) ReadyController.instance.setPlayer1Ready(true);
                        else ReadyController.instance.setPlayer2Ready(true);
                    }
                });
            }

            @Override
            public void onBothReady() {
                Platform.runLater(() -> {
                    String n1 = SessionManager.getPlayer1Name();
                    String n2 = SessionManager.getPlayer2Name();
                    if (n1.isEmpty()) n1 = "Player 1";
                    if (n2.isEmpty()) n2 = "Player 2";
                    Wizard w1 = new Wizard(n1, 200, WizardSkin.EMBER_MAGE, ArenaBackground.DARK_TOWER);
                    Wizard w2 = new Wizard(n2, 200, WizardSkin.FROST_WITCH, ArenaBackground.DARK_TOWER);
                    WheelController.setWizards(w1, w2);
                    WheelController.setNetworkMode(true);
                    Main.navigateTo("wheel-view.fxml");
                });
            }

            @Override
            public void onWheelStart() {}

            @Override
            public void onWheelMana(int playerId, int mana) {
                Platform.runLater(() -> WheelController.updateOpponentMana(playerId, mana));
            }

            @Override
            public void onWheelOvertime(int seconds) {
                Platform.runLater(() -> WheelController.addOvertime(seconds));
            }

            @Override
            public void onWheelResult(int firstPlayerId) {}

            @Override
            public void onGameStart(String name1, String name2, int firstPlayerId, long gridSeed) {
                Platform.runLater(() -> {
                    WheelController.stopWheel();
                    String n1 = (name1 == null || name1.isEmpty()) ? SessionManager.getPlayer1Name() : name1;
                    String n2 = (name2 == null || name2.isEmpty()) ? SessionManager.getPlayer2Name() : name2;
                    if (n1.isEmpty()) n1 = "Player 1";
                    if (n2.isEmpty()) n2 = "Player 2";
                    Wizard w1 = new Wizard(n1, 200, WizardSkin.EMBER_MAGE, ArenaBackground.DARK_TOWER);
                    Wizard w2 = new Wizard(n2, 200, WizardSkin.FROST_WITCH, ArenaBackground.DARK_TOWER);
                    Wizard first = firstPlayerId == 1 ? w1 : w2;
                    Wizard second = firstPlayerId == 1 ? w2 : w1;
                    GameController.setWizards(first, second);
                    NetworkGameController.setSession(SessionManager.getClient(), SessionManager.getMyPlayerId(), w1, w2, gridSeed);
                    Main.navigateTo("network-game-view.fxml");
                });
            }

            @Override
            public void onGridSeed(long seed) {
                Platform.runLater(() -> NetworkGameController.applyGridSeed(seed));
            }

            @Override
            public void onShuffleGrid(String letters) {
                Platform.runLater(() -> NetworkGameController.applyShuffleGrid(letters));
            }

            @Override
            public void onHalfHpPrompt(int playerId) {
                Platform.runLater(() -> NetworkGameController.onHalfHpPrompt(playerId));
            }

            @Override
            public void onHalfHpStart(long gridSeed) {
                Platform.runLater(() -> NetworkGameController.onHalfHpStart(gridSeed));
            }

            @Override
            public void onHalfHpResult(int initiatorWon, int damageOrHeal, int p1Hp, int p2Hp) {
                Platform.runLater(() -> NetworkGameController.onHalfHpResult(initiatorWon, damageOrHeal, p1Hp, p2Hp));
            }

            @Override
            public void onLastStandStart(String scrambledWord) {
                Platform.runLater(() -> NetworkGameController.onLastStandStart(scrambledWord));
            }

            @Override
            public void onLastStandResult(int winnerId, int hpGain, int p1Hp, int p2Hp) {
                Platform.runLater(() -> NetworkGameController.onLastStandResult(winnerId, hpGain, p1Hp, p2Hp));
            }

            @Override
            public void onState(int p1Hp, int p2Hp, int currentTurn, String lastSpellDesc) {
                Platform.runLater(() -> NetworkGameController.updateState(p1Hp, p2Hp, currentTurn, lastSpellDesc));
            }

            @Override
            public void onInvalidWord(String word) {
                Platform.runLater(() -> NetworkGameController.showInvalidWord(word));
            }

            @Override
            public void onTurnStart(int currentTurn) {
                Platform.runLater(() -> NetworkGameController.onTurnStart(currentTurn));
            }

            @Override
            public void onTurnTick(int secondsLeft) {
                Platform.runLater(() -> NetworkGameController.onTurnTick(secondsLeft));
            }

            @Override
            public void onTurnExpired() {
                Platform.runLater(NetworkGameController::onTurnExpired);
            }

            @Override
            public void onGameOver(int winnerId) {
                Platform.runLater(() -> NetworkGameController.onGameOver(winnerId));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> {
                    statusLabel.setText(message);
                    statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                });
            }
        });
        if (!client.connect()) {
            statusLabel.setText("Connection failed. Check IP and that host is running.");
            statusLabel.setStyle("-fx-text-fill: #ff6b6b;");
            return;
        }
        client.sendJoin(name);
    }

    @FXML
    private void onBackClicked() {
        SessionManager.clear();
        if (server != null) {
            server.stop();
            server = null;
        }
        Main.navigateTo("menu-view.fxml");
    }

    private void displayLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Check for IPv4 and non-loopback
                    if (addr.isSiteLocalAddress()) {
                        localIpLabel.setText("Your IP for hosting: " + addr.getHostAddress());
                        return;
                    }
                }
            }
            localIpLabel.setText("Your IP for hosting: Not found");
        } catch (SocketException e) {
            localIpLabel.setText("Could not determine local IP.");
        }
    }
}
