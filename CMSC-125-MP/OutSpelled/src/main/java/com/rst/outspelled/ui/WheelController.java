package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.Wizard;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WheelController {

    @FXML private Canvas wheelCanvas;
    @FXML private Label player1NameLabel;
    @FXML private Label player2NameLabel;
    @FXML private Label player1ManaLabel;
    @FXML private Label player2ManaLabel;
    @FXML private Label player1FeedbackLabel;
    @FXML private Label player2FeedbackLabel;
    @FXML private Label timerLabel;
    @FXML private Label statusLabel;

    private static Wizard wizard1;
    private static Wizard wizard2;

    // Wheel state
    private double needleAngle = 0;
    private double needleSpeed = 3.0;
    private double spinDirection = 1; // 1 = CW, -1 = CCW
    private double zoneStartAngle = 0;
    private static final double ZONE_SIZE = 35; // degrees

    // Mana: +10 on hit, -5 on miss (min 0)
    private static final int MANA_ON_HIT = 10;
    private static final int MANA_ON_MISS = 5;
    private int player1Mana = 0;
    private int player2Mana = 0;

    // Timer and state
    private int secondsRemaining = 15;
    private boolean gameActive = false;
    private int countdownValue = 3;

    private final java.util.Random random = new java.util.Random();
    private boolean testToggle = true; // local 2P: alternate which player gets spacebar
    private static boolean isNetworkMode = false;
    private AnimationTimer animationTimer;
    private ScheduledExecutorService timerExecutor;

    public static void setWizards(Wizard w1, Wizard w2) {
        wizard1 = w1;
        wizard2 = w2;
    }

    public static void setNetworkMode(boolean network) {
        isNetworkMode = network;
    }

    private void sendWheelManaToServer(int playerId, int mana) {
        com.rst.outspelled.network.GameClient c = com.rst.outspelled.network.SessionManager.getClient();
        if (c != null && com.rst.outspelled.network.SessionManager.getMyPlayerId() == playerId)
            c.sendWheelMana(mana);
    }

    private void sendWheelScoreToServer() {
        com.rst.outspelled.network.GameClient c = com.rst.outspelled.network.SessionManager.getClient();
        if (c != null) {
            int myId = com.rst.outspelled.network.SessionManager.getMyPlayerId();
            int myMana = myId == 1 ? player1Mana : player2Mana;
            c.sendWheelScore(myMana);
        }
    }

    public static void updateOpponentMana(int playerId, int mana) {
        Platform.runLater(() -> {
            if (instance != null) {
                if (playerId == 1) instance.player1ManaLabel.setText(String.valueOf(mana));
                else instance.player2ManaLabel.setText(String.valueOf(mana));
            }
        });
    }

    private static WheelController instance;
    private volatile boolean waitingForWheelResult = false;

    public static void addOvertime(int seconds) {
        Platform.runLater(() -> {
            if (instance != null) {
                instance.waitingForWheelResult = false;
                instance.secondsRemaining += seconds;
                instance.statusLabel.setText("Tie! +" + seconds + " sec overtime!");
                instance.timerLabel.setText(String.valueOf(instance.secondsRemaining));
            }
        });
    }

    /** Stop wheel timer when navigating away (e.g. on GAME_START). */
    public static void stopWheel() {
        Platform.runLater(() -> {
            if (instance != null && instance.timerExecutor != null) {
                instance.timerExecutor.shutdownNow();
                instance.timerExecutor = null;
            }
        });
    }

    @FXML
    public void initialize() {
        instance = this;
        waitingForWheelResult = false;
        player1NameLabel.setText(wizard1.getName());
        player2NameLabel.setText(wizard2.getName());
        zoneStartAngle = random.nextDouble() * 360;

        // draw initial wheel before countdown finishes
        drawWheel();
        startCountdown();
    }

    // --- Countdown ---

    private void startCountdown() {
        statusLabel.setText("Starting in " + countdownValue + "...");

        ScheduledExecutorService countdown =
                Executors.newSingleThreadScheduledExecutor();
        countdown.scheduleAtFixedRate(() -> {
            countdownValue--;
            Platform.runLater(() -> {
                if (countdownValue > 0) {
                    statusLabel.setText("Starting in " + countdownValue + "...");
                } else {
                    statusLabel.setText("GO! Hit the zone!");
                    gameActive = true;
                    startWheelAnimation();
                    startGameTimer();
                    setupKeyListener();
                    countdown.shutdown();
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    // --- Wheel Animation ---

    private void startWheelAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                needleAngle += needleSpeed * spinDirection;
                if (needleAngle >= 360) needleAngle -= 360;
                if (needleAngle < 0) needleAngle += 360;
                drawWheel();
            }
        };
        animationTimer.start();
    }

    private void drawWheel() {
        GraphicsContext gc = wheelCanvas.getGraphicsContext2D();
        double w = wheelCanvas.getWidth();
        double h = wheelCanvas.getHeight();
        double cx = w / 2;
        double cy = h / 2;
        double radius = 120;
        double strokeWidth = 25;

        // clear canvas
        gc.clearRect(0, 0, w, h);

        // base ring
        gc.setStroke(Color.web("#2a2a4a"));
        gc.setLineWidth(strokeWidth);
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // colored zone arc
        gc.setStroke(Color.web("#e2b96f"));
        gc.setLineWidth(strokeWidth);
        gc.strokeArc(
                cx - radius, cy - radius,
                radius * 2, radius * 2,
                -zoneStartAngle, -ZONE_SIZE,
                ArcType.OPEN
        );

        // needle
        double needleRad = Math.toRadians(needleAngle);
        double needleLength = radius - 10;
        double nx = cx + Math.cos(needleRad) * needleLength;
        double ny = cy + Math.sin(needleRad) * needleLength;

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeLine(cx, cy, nx, ny);

        // center dot
        gc.setFill(Color.web("#e2b96f"));
        gc.fillOval(cx - 6, cy - 6, 12, 12);
    }

    // --- Game Timer Thread ---

    private void startGameTimer() {
        timerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("WheelTimerThread");
            t.setDaemon(true);
            return t;
        });

        timerExecutor.scheduleAtFixedRate(() -> {
            if (waitingForWheelResult) return;
            secondsRemaining--;
            Platform.runLater(() -> {
                if (waitingForWheelResult) return;
                timerLabel.setText(String.valueOf(Math.max(0, secondsRemaining)));
                if (secondsRemaining <= 5) {
                    timerLabel.setStyle(
                            "-fx-text-fill: #ff6b6b;" +
                                    "-fx-font-size: 36px;" +
                                    "-fx-font-weight: bold;");
                }
                if (secondsRemaining <= 0) {
                    if (isNetworkMode) {
                        sendWheelScoreToServer();
                        waitingForWheelResult = true;
                        return;
                    }
                    if (player1Mana == player2Mana) {
                        secondsRemaining += 5;
                        statusLabel.setText("Tie! +5 sec overtime!");
                        timerLabel.setText(String.valueOf(secondsRemaining));
                        return;
                    }
                    endGame();
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    // --- Key Listener ---

    private void setupKeyListener() {
        wheelCanvas.getScene().addEventFilter(
                javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() != KeyCode.SPACE || !gameActive) return;
                    if (isNetworkMode) {
                        // Only local player's clicks count; my side is wizard1 or wizard2 by who went first
                        int myId = com.rst.outspelled.network.SessionManager.getMyPlayerId();
                        Wizard me = myId == 1 ? wizard1 : wizard2;
                        handlePlayerClick(me);
                    } else {
                        handlePlayerClick(testToggle ? wizard1 : wizard2);
                        testToggle = !testToggle;
                    }
                    event.consume();
                }
        );
    }

    // --- Click Handling ---

    private void handlePlayerClick(Wizard player) {
        if (!gameActive) return;

        boolean isHit = isNeedleInZone();

        if (player == wizard1) {
            if (isHit) {
                player1Mana += MANA_ON_HIT;
                showFeedback(player1FeedbackLabel, "✅ +" + MANA_ON_HIT);
            } else {
                player1Mana = Math.max(0, player1Mana - MANA_ON_MISS);
                showFeedback(player1FeedbackLabel, "❌ -" + MANA_ON_MISS);
            }
            player1ManaLabel.setText(String.valueOf(player1Mana));
            if (isNetworkMode) sendWheelManaToServer(1, player1Mana);
        } else {
            if (isHit) {
                player2Mana += MANA_ON_HIT;
                showFeedback(player2FeedbackLabel, "✅ +" + MANA_ON_HIT);
            } else {
                player2Mana = Math.max(0, player2Mana - MANA_ON_MISS);
                showFeedback(player2FeedbackLabel, "❌ -" + MANA_ON_MISS);
            }
            player2ManaLabel.setText(String.valueOf(player2Mana));
            if (isNetworkMode) sendWheelManaToServer(2, player2Mana);
        }

        if (isHit) {
            // relocate zone and switch direction on successful hit
            zoneStartAngle = random.nextDouble() * 360;
            spinDirection = random.nextBoolean() ? 1 : -1;
            needleSpeed = 2.5 + random.nextDouble() * 2.5;
        }
    }

    private boolean isNeedleInZone() {
        double needle = ((needleAngle % 360) + 360) % 360;
        double zoneEnd = (zoneStartAngle + ZONE_SIZE) % 360;

        if (zoneStartAngle <= zoneEnd) {
            return needle >= zoneStartAngle && needle <= zoneEnd;
        } else {
            // zone wraps around 360
            return needle >= zoneStartAngle || needle <= zoneEnd;
        }
    }

    private void showFeedback(Label label, String text) {
        label.setText(text);
        new Thread(() -> {
            try {
                Thread.sleep(600);
                Platform.runLater(() -> label.setText(""));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // --- End Game ---

    private void endGame() {
        if (!gameActive) return;
        gameActive = false;

        if (animationTimer != null) animationTimer.stop();
        if (timerExecutor != null) timerExecutor.shutdown();

        Wizard winner;
        String resultText;

        if (player1Mana > player2Mana) {
            winner = wizard1;
            resultText = wizard1.getName() + " wins the Mana Wheel! They go first!";
        } else if (player2Mana > player1Mana) {
            winner = wizard2;
            resultText = wizard2.getName() + " wins the Mana Wheel! They go first!";
        } else {
            winner = wizard1;
            resultText = "It's a tie! " + wizard1.getName() + " goes first by default!";
        }

        timerLabel.setText("0");
        statusLabel.setText(resultText);
        statusLabel.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 14px;");

        if (isNetworkMode) {
            com.rst.outspelled.network.GameClient client = com.rst.outspelled.network.SessionManager.getClient();
            int myMana = com.rst.outspelled.network.SessionManager.getMyPlayerId() == 1 ? player1Mana : player2Mana;
            if (client != null) client.sendWheelScore(myMana);
            // Navigation to game is done when we receive GAME_START from server
            return;
        }

        // Local: winner goes first — pass in correct order to GameController
        Wizard first = winner;
        Wizard second = winner == wizard1 ? wizard2 : wizard1;
        GameController.setWizards(first, second);

        new Thread(() -> {
            try {
                Thread.sleep(2500);
                Platform.runLater(() -> Main.navigateTo("game-view.fxml"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}