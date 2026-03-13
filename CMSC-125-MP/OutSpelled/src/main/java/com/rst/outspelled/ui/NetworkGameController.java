package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.LetterTile;
import com.rst.outspelled.model.Spell;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.network.GameClient;
import com.rst.outspelled.network.SessionManager;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Game view for LAN: same letter-grid gameplay as local; state from server, words sent to server.
 */
public class NetworkGameController {

    @FXML private Label player1NameLabel;
    @FXML private Label player2NameLabel;
    @FXML private HBox player1Hearts;
    @FXML private HBox player2Hearts;
    @FXML private Label timerLabel;
    @FXML private Label turnLabel;
    @FXML private HBox floatingLettersPane;
    @FXML private GridPane letterGridPane;
    @FXML private Button castButton;
    @FXML private Label feedbackLabel;
    @FXML private Rectangle wizard1Portrait;
    @FXML private Rectangle wizard2Portrait;
    @FXML private Label wizard1Label;
    @FXML private Label wizard2Label;
    @FXML private Button shuffleButton;
    @FXML private Button menuButton;
    @FXML private Label skillCheckStatusLabel;
    @FXML private StackPane lastStandOverlay;
    @FXML private Label scrambledWordLabel;
    @FXML private Label lastStandTimerLabel;
    @FXML private TextField lastStandInput;
    @FXML private Label lastStandFeedbackLabel;
    @FXML private ListView<String> battleLog;

    private static GameClient client;
    private static int myPlayerId;
    private static Wizard wizard1;
    private static Wizard wizard2;
    private static int currentTurn = 1;
    private static final int MAX_HP = 200;
    private static final int HEARTS_COUNT = 10;
    private static final int HP_PER_HEART = MAX_HP / HEARTS_COUNT;
    private static final int SKILL_CHECK_SEC = 15;

    private LetterGrid letterGrid;
    private final List<Label> player1HeartLabels = new ArrayList<>();
    private final List<Label> player2HeartLabels = new ArrayList<>();
    private boolean halfHpChallengeActive = false;
    private ScheduledExecutorService skillCheckTimer;
    private volatile int lastStandSecondsLeft = SKILL_CHECK_SEC;

    public static void setSession(GameClient c, int myId, Wizard w1, Wizard w2, long gridSeed) {
        client = c;
        myPlayerId = myId;
        wizard1 = w1;
        wizard2 = w2;
        initialGridSeed = gridSeed;
    }

    private static long initialGridSeed;

    @FXML
    public void initialize() {
        instance = this;
        letterGrid = new LetterGrid(initialGridSeed);
        if (wizard1 != null) {
            player1NameLabel.setText(wizard1.getName());
            if (wizard1Label != null) wizard1Label.setText(wizard1.getName());
        }
        if (wizard2 != null) {
            player2NameLabel.setText(wizard2.getName());
            if (wizard2Label != null) wizard2Label.setText(wizard2.getName());
        }
        buildHearts();
        updateHearts(MAX_HP, MAX_HP);
        turnLabel.setText(wizard1 != null ? wizard1.getName() + "'s Turn" : "Player 1's Turn");
        feedbackLabel.setText("");
        renderGrid();
        updateSelectedWordDisplay();
        updateInputEnabled();
        updatePortraitHighlight();
        Platform.runLater(this::setupKeyboardHandler);
        if (skillCheckStatusLabel != null) skillCheckStatusLabel.setText("");
        if (lastStandOverlay != null) lastStandOverlay.setVisible(false);
    }

    public static void applyShuffleGrid(String letters) {
        Platform.runLater(() -> {
            if (instance != null && instance.letterGrid != null && letters != null && letters.length() >= 16) {
                instance.letterGrid.applyLayout(letters);
                instance.renderGrid();
                instance.updateSelectedWordDisplay();
                instance.feedbackLabel.setText("Grid shuffled!");
                instance.feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
            }
        });
    }

    public static void applyGridSeed(long seed) {
        Platform.runLater(() -> {
            if (instance != null && instance.letterGrid != null) {
                instance.letterGrid.resetWithSeed(seed);
                instance.letterGrid.deselectAll();
                instance.renderGrid();
                instance.updateSelectedWordDisplay();
            }
        });
    }

    private void setupKeyboardHandler() {
        if (letterGridPane.getScene() == null) return;
        letterGridPane.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (letterGridPane.isDisabled()) return;
            handleKeyInput(event);
            event.consume();
        });
    }

    private void handleKeyInput(javafx.scene.input.KeyEvent event) {
        String key = event.getText();
        javafx.scene.input.KeyCode code = event.getCode();
        if (code == javafx.scene.input.KeyCode.BACK_SPACE) {
            String word = letterGrid.getSelectedWord();
            if (!word.isEmpty()) {
                char last = word.charAt(word.length() - 1);
                letterGrid.deselectLastMatchingTile(last);
                renderGrid();
                updateSelectedWordDisplay();
            }
            return;
        }
        if (code == javafx.scene.input.KeyCode.ENTER) {
            onCastClicked();
            return;
        }
        if (code == javafx.scene.input.KeyCode.ESCAPE) {
            onClearClicked();
            return;
        }
        if (code == javafx.scene.input.KeyCode.SPACE) {
            if (!shuffleButton.isDisabled()) onShuffleClicked();
            return;
        }
        if (key != null && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            LetterTile matched = letterGrid.selectFirstMatchingTile(key.charAt(0));
            if (matched != null) {
                renderGrid();
                updateSelectedWordDisplay();
            } else {
                feedbackLabel.setText("Letter '" + key.toUpperCase() + "' not available!");
                feedbackLabel.setStyle("-fx-text-fill: #ff6b6b;");
            }
        }
    }

    private void renderGrid() {
        letterGridPane.getChildren().clear();
        for (int r = 0; r < letterGrid.getRows(); r++) {
            for (int c = 0; c < letterGrid.getCols(); c++) {
                LetterTile tile = letterGrid.getTile(r, c);
                Button btn = createTileButton(tile, r, c);
                letterGridPane.add(btn, c, r);
            }
        }
    }

    private Button createTileButton(LetterTile tile, int row, int col) {
        Button btn = new Button(String.valueOf(tile.getLetter()) + "\n" + tile.getValue());
        btn.setPrefSize(62, 62);
        btn.setStyle(tile.isSelected() ? getSelectedStyle() : getIdleStyle());
        btn.setOnAction(e -> {
            if (tile.isSelected()) {
                letterGrid.deselectTile(row, col);
                btn.setStyle(getIdleStyle());
            } else if (tile.isIdle()) {
                letterGrid.selectTile(row, col);
                btn.setStyle(getSelectedStyle());
            }
            updateSelectedWordDisplay();
        });
        return btn;
    }

    private static String getIdleStyle() {
        return "-fx-background-color: #2a2a4a; -fx-text-fill: #e2b96f; -fx-font-size: 14px; -fx-font-weight: bold;"
                + " -fx-border-color: #444466; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private static String getSelectedStyle() {
        return "-fx-background-color: #e2b96f; -fx-text-fill: #1a1a2e; -fx-font-size: 14px; -fx-font-weight: bold;"
                + " -fx-border-color: #ffffff; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private void buildHearts() {
        if (player1Hearts == null || player2Hearts == null) return;
        player1Hearts.getChildren().clear();
        player2Hearts.getChildren().clear();
        player1HeartLabels.clear();
        player2HeartLabels.clear();
        for (int i = 0; i < HEARTS_COUNT; i++) {
            Label h1 = new Label("♥");
            h1.setStyle("-fx-text-fill: #e24a4a; -fx-font-size: 18px;");
            player1Hearts.getChildren().add(h1);
            player1HeartLabels.add(h1);
            Label h2 = new Label("♥");
            h2.setStyle("-fx-text-fill: #e24a4a; -fx-font-size: 18px;");
            player2Hearts.getChildren().add(h2);
            player2HeartLabels.add(h2);
        }
    }

    private void updateHearts(int p1Hp, int p2Hp) {
        for (int i = 0; i < HEARTS_COUNT; i++) {
            if (i < player1HeartLabels.size()) {
                boolean full = p1Hp > i * HP_PER_HEART;
                player1HeartLabels.get(i).setStyle(full ? "-fx-text-fill: #e24a4a; -fx-font-size: 18px;" : "-fx-text-fill: #442020; -fx-font-size: 18px;");
            }
            if (i < player2HeartLabels.size()) {
                boolean full = p2Hp > i * HP_PER_HEART;
                player2HeartLabels.get(i).setStyle(full ? "-fx-text-fill: #e24a4a; -fx-font-size: 18px;" : "-fx-text-fill: #442020; -fx-font-size: 18px;");
            }
        }
    }

    private void updateSelectedWordDisplay() {
        String word = letterGrid.getSelectedWord();
        updateFloatingLetters(word);
        updateDamagePreview(word);
    }

    private void updateFloatingLetters(String word) {
        if (floatingLettersPane == null) return;
        floatingLettersPane.getChildren().clear();
        if (word == null || word.isEmpty()) return;
        for (char c : word.toUpperCase().toCharArray()) {
            Label l = new Label(String.valueOf(c));
            l.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 28px; -fx-font-weight: bold; -fx-background-color: #2a2a4a; -fx-padding: 8 12; -fx-border-color: #444466; -fx-border-radius: 6;");
            floatingLettersPane.getChildren().add(l);
        }
        startFloatingAnimation();
    }

    private void startFloatingAnimation() {
        if (floatingLettersPane == null || floatingLettersPane.getChildren().isEmpty()) return;
        for (int i = 0; i < floatingLettersPane.getChildren().size(); i++) {
            javafx.scene.Node n = floatingLettersPane.getChildren().get(i);
            TranslateTransition tt = new TranslateTransition(Duration.millis(700 + i * 60), n);
            tt.setFromY(0);
            tt.setToY(-5);
            tt.setAutoReverse(true);
            tt.setCycleCount(TranslateTransition.INDEFINITE);
            tt.play();
        }
    }

    private int currentP1Hp = MAX_HP;
    private int currentP2Hp = MAX_HP;
    private javafx.animation.Timeline blinkTimeline;

    private void updateDamagePreview(String word) {
        if (blinkTimeline != null) blinkTimeline.stop();
        updateHearts(currentP1Hp, currentP2Hp);
        if (word == null || word.length() < 3 || currentTurn != myPlayerId || halfHpChallengeActive) return;
        int damage = new Spell(word, "").getTotalDamage();
        int heartsToBlink = Math.min(HEARTS_COUNT, (int) Math.ceil(damage / (double) HP_PER_HEART));
        List<Label> toBlink = (myPlayerId == 1) ? player2HeartLabels : player1HeartLabels;
        int startIdx = HEARTS_COUNT - heartsToBlink;
        if (startIdx < 0) startIdx = 0;
        final int start = startIdx;
        int oppFullHearts = Math.min(HEARTS_COUNT, (myPlayerId == 1 ? currentP2Hp : currentP1Hp) / HP_PER_HEART);
        final String fullStyle = "-fx-text-fill: #e24a4a; -fx-font-size: 18px;";
        final String dimStyle = "-fx-text-fill: #442020; -fx-font-size: 18px;";
        blinkTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(0), e -> {
                    for (int i = start; i < toBlink.size(); i++) toBlink.get(i).setStyle("-fx-text-fill: #ff8888; -fx-font-size: 18px;");
                }),
                new javafx.animation.KeyFrame(Duration.millis(400), e -> {
                    for (int i = start; i < toBlink.size(); i++)
                        toBlink.get(i).setStyle(i < oppFullHearts ? fullStyle : dimStyle);
                })
        );
        blinkTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        blinkTimeline.setAutoReverse(true);
        blinkTimeline.play();
    }

    public static void updateState(int p1Hp, int p2Hp, int turn, String lastSpellDesc) {
        currentTurn = turn;
        Platform.runLater(() -> {
            if (instance != null) {
                instance.currentP1Hp = p1Hp;
                instance.currentP2Hp = p2Hp;
                instance.updateHearts(p1Hp, p2Hp);
                instance.turnLabel.setText(instance.currentTurnName() + "'s Turn");
                instance.updateInputEnabled();
                instance.updatePortraitHighlight();
                instance.updateDamagePreview(
                        instance.letterGrid != null ? instance.letterGrid.getSelectedWord() : "");

                if (instance.battleLog != null
                        && lastSpellDesc != null
                        && !lastSpellDesc.isBlank()) {
                    String entry = "⚔ " + lastSpellDesc;
                    if (instance.battleLog.getItems().isEmpty()
                            || !instance.battleLog.getItems().get(0).equals(entry)) {
                        instance.battleLog.getItems().add(0, entry);
                    }
                }
            }
        });
    }

    private void updatePortraitHighlight() {
        if (wizard1Portrait == null || wizard2Portrait == null) return;
        final double activeStroke = 6;
        final double inactiveStroke = 1.5;
        final String activeColor = "#e2b96f";
        final String inactiveColor = "#444466";
        if (currentTurn == 1) {
            wizard1Portrait.setStroke(Color.web(activeColor));
            wizard1Portrait.setStrokeWidth(activeStroke);
            wizard2Portrait.setStroke(Color.web(inactiveColor));
            wizard2Portrait.setStrokeWidth(inactiveStroke);
        } else {
            wizard2Portrait.setStroke(Color.web(activeColor));
            wizard2Portrait.setStrokeWidth(activeStroke);
            wizard1Portrait.setStroke(Color.web(inactiveColor));
            wizard1Portrait.setStrokeWidth(inactiveStroke);
        }
    }

    private static NetworkGameController instance;

    private String currentTurnName() {
        return currentTurn == 1 ? (wizard1 != null ? wizard1.getName() : "Player 1")
                : (wizard2 != null ? wizard2.getName() : "Player 2");
    }

    private void updateInputEnabled() {
        boolean myTurn = (currentTurn == myPlayerId) || halfHpChallengeActive;
        castButton.setDisable(!myTurn);
        letterGridPane.setDisable(!myTurn);
        if (shuffleButton != null) shuffleButton.setDisable(!myTurn || halfHpChallengeActive);
    }

    public static void showInvalidWord(String word) {
        Platform.runLater(() -> {
            if (instance != null) {
                instance.feedbackLabel.setText("\"" + word + "\" is not valid.");
                instance.feedbackLabel.setStyle("-fx-text-fill: #ff6b6b;");
                instance.castButton.setDisable(false);
                instance.letterGridPane.setDisable(false);
            }
        });
    }

    public static void onTurnStart(int turn) {
        currentTurn = turn;
        Platform.runLater(() -> {
            if (instance != null) {
                instance.turnLabel.setText(instance.currentTurnName() + "'s Turn");
                instance.updateInputEnabled();
                instance.updatePortraitHighlight();
            }
        });
    }

    public static void onTurnTick(int secondsLeft) {
        Platform.runLater(() -> {
            if (instance != null) {
                instance.timerLabel.setText(String.valueOf(secondsLeft));
                instance.timerLabel.setStyle(secondsLeft <= 10
                        ? "-fx-text-fill: #ff6b6b; -fx-font-size: 28px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #e2b96f; -fx-font-size: 28px; -fx-font-weight: bold;");
            }
        });
    }

    public static void onTurnExpired() {
        Platform.runLater(() -> {
            if (instance != null) {
                instance.updateInputEnabled();
            }
        });
    }

    public static void onHalfHpPrompt(int playerId) {
        Platform.runLater(() -> {
            if (instance == null || client == null) return;
            if (myPlayerId != playerId) return;
            String name = playerId == 1 && wizard1 != null ? wizard1.getName() : (wizard2 != null ? wizard2.getName() : "You");
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Skill Check Available!");
            alert.setHeaderText(name + " can initiate a challenge!");
            alert.setContentText(
                    "You are at or below 50% HP.\n\n" +
                            "Initiate the Half HP Challenge?\n" +
                            "Win → opponent drops to your HP level\n" +
                            "Lose → you take word damage\n\n" +
                            "This can only be used once per match!"
            );
            ButtonType initiate = new ButtonType("⚔ Initiate!");
            ButtonType skip = new ButtonType("Skip");
            alert.getButtonTypes().setAll(initiate, skip);
            alert.showAndWait().ifPresent(response -> {
                if (response == initiate) client.sendHalfHpInitiate();
                else client.sendHalfHpSkip();
            });
        });
    }

    public static void onHalfHpStart(long gridSeed) {
        Platform.runLater(() -> {
            if (instance != null && instance.letterGrid != null) {
                instance.halfHpChallengeActive = true;
                instance.letterGrid.resetWithSeed(gridSeed);
                instance.letterGrid.deselectAll();
                instance.renderGrid();
                instance.updateSelectedWordDisplay();
                if (instance.skillCheckStatusLabel != null) {
                    instance.skillCheckStatusLabel.setText("⚔ HALF HP CHALLENGE — Both players spell a word!");
                    instance.skillCheckStatusLabel.setStyle("-fx-text-fill: #ff6b6b;");
                }
                instance.turnLabel.setText("⚔ SKILL CHECK");
                instance.feedbackLabel.setText("Select letters and cast your best spell!");
                instance.feedbackLabel.setStyle("-fx-text-fill: #e2b96f;");
                instance.updateInputEnabled();
            }
        });
    }

    public static void onHalfHpResult(int initiatorWon, int damageOrHeal, int p1Hp, int p2Hp) {
        Platform.runLater(() -> {
            if (instance != null) {
                instance.halfHpChallengeActive = false;
                instance.currentP1Hp = p1Hp;
                instance.currentP2Hp = p2Hp;
                instance.updateHearts(p1Hp, p2Hp);
                String summary = initiatorWon == 1 ? "Initiator wins! Opponent takes damage."
                        : "Initiator loses! They take " + damageOrHeal + " damage.";
                if (instance.skillCheckStatusLabel != null) {
                    instance.skillCheckStatusLabel.setText("⚔ " + summary);
                    instance.skillCheckStatusLabel.setStyle("-fx-text-fill: #e2b96f;");
                }
                if (instance.battleLog != null) {
                    instance.battleLog.getItems().add(0, "⚔ Half HP Challenge: " + summary);
                }
                instance.updateInputEnabled();
            }
        });
    }

    public static void onLastStandStart(String scrambledWord) {
        Platform.runLater(() -> {
            if (instance != null) {
                if (instance.lastStandOverlay != null) instance.lastStandOverlay.setVisible(true);
                if (instance.scrambledWordLabel != null) instance.scrambledWordLabel.setText(scrambledWord != null ? scrambledWord : "??????");
                if (instance.lastStandFeedbackLabel != null) instance.lastStandFeedbackLabel.setText("");
                if (instance.lastStandInput != null) {
                    instance.lastStandInput.clear();
                    instance.lastStandInput.setDisable(false);
                    instance.lastStandInput.setOnKeyPressed(e -> {
                        if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                            String attempt = instance.lastStandInput.getText().trim();
                            if (!attempt.isEmpty() && client != null) {
                                client.sendLastStandWord(attempt);
                                instance.lastStandFeedbackLabel.setText("Answer submitted!");
                                instance.lastStandFeedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
                                instance.lastStandInput.setDisable(true);
                            }
                        }
                    });
                    Platform.runLater(() -> instance.lastStandInput.requestFocus());
                }
                instance.lastStandSecondsLeft = SKILL_CHECK_SEC;
                if (instance.lastStandTimerLabel != null) instance.lastStandTimerLabel.setText(String.valueOf(SKILL_CHECK_SEC));
                if (instance.skillCheckTimer != null) instance.skillCheckTimer.shutdown();
                instance.skillCheckTimer = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                });
                instance.skillCheckTimer.scheduleAtFixedRate(() -> {
                    instance.lastStandSecondsLeft--;
                    Platform.runLater(() -> {
                        if (instance.lastStandTimerLabel != null)
                            instance.lastStandTimerLabel.setText(String.valueOf(Math.max(0, instance.lastStandSecondsLeft)));
                    });
                }, 1, 1, TimeUnit.SECONDS);
            }
        });
    }

    public static void onLastStandResult(int winnerId, int hpGain, int p1Hp, int p2Hp) {
        Platform.runLater(() -> {
            if (instance != null) {
                if (instance.skillCheckTimer != null) {
                    instance.skillCheckTimer.shutdownNow();
                    instance.skillCheckTimer = null;
                }
                if (instance.lastStandOverlay != null) instance.lastStandOverlay.setVisible(false);
                if (instance.lastStandInput != null) instance.lastStandInput.setDisable(false);
                instance.currentP1Hp = p1Hp;
                instance.currentP2Hp = p2Hp;
                instance.updateHearts(p1Hp, p2Hp);
                String summary = winnerId == 0 ? "No one solved it in time."
                        : (winnerId == myPlayerId ? "You" : (winnerId == 1 ? (wizard1 != null ? wizard1.getName() : "Player 1") : (wizard2 != null ? wizard2.getName() : "Player 2")))
                        + " solved it! +" + hpGain + " HP.";
                if (instance.skillCheckStatusLabel != null) {
                    instance.skillCheckStatusLabel.setText(summary);
                    instance.skillCheckStatusLabel.setStyle("-fx-text-fill: #e2b96f;");
                }
                if (instance.battleLog != null) {
                    instance.battleLog.getItems().add(0, "🛡 Last Stand: " + summary);
                }
            }
        });
    }

    public static void onGameOver(int winnerId) {
        Platform.runLater(() -> {
            if (instance != null) {
                String winnerName = winnerId == 1 ? (wizard1 != null ? wizard1.getName() : "Player 1")
                        : (wizard2 != null ? wizard2.getName() : "Player 2");
                instance.feedbackLabel.setText(winnerName + " wins!");
                instance.feedbackLabel.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 16px;");
                instance.castButton.setDisable(true);
                instance.letterGridPane.setDisable(true);
                if (instance.menuButton != null) instance.menuButton.setDisable(false);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Over");
                alert.setHeaderText(winnerName + " wins!");
                alert.showAndWait();
                Main.navigateTo("menu-view.fxml");
            }
        });
    }

    @FXML
    private void onCastClicked() {
        String word = letterGrid.getSelectedWord();
        if (word.length() < 3) {
            feedbackLabel.setText("Word must be at least 3 letters!");
            feedbackLabel.setStyle("-fx-text-fill: #ff6b6b;");
            return;
        }
        if (client != null) {
            if (halfHpChallengeActive) {
                client.sendHalfHpWord(word);
                letterGrid.deselectAll();
                renderGrid();
                updateSelectedWordDisplay();
                feedbackLabel.setText("Word submitted! Waiting for opponent...");
                feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
                castButton.setDisable(true);
                return;
            }
            client.sendWord(word);

            renderGrid();
            updateSelectedWordDisplay();
            feedbackLabel.setText("Casting...");
            feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
            castButton.setDisable(true);
            letterGridPane.setDisable(true);
        }
    }

    @FXML
    private void onClearClicked() {
        letterGrid.deselectAll();
        renderGrid();
        updateSelectedWordDisplay();
        feedbackLabel.setText("");
    }

    @FXML
    private void onShuffleClicked() {
        if (shuffleButton != null && shuffleButton.isDisabled()) return;
        if (!letterGrid.getSelectedWord().isEmpty()) {
            letterGrid.shuffleIdleTilesOnly();
        } else {
            letterGrid.shuffleGrid();
        }
        if (client != null) {
            client.sendShuffle(letterGrid.getLettersAsString());
        }
        renderGrid();
        updateSelectedWordDisplay();
        feedbackLabel.setText("Grid shuffled!");
        feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
    }

    @FXML
    private void onMenuClicked() {
        SessionManager.clear();
        Main.navigateTo("menu-view.fxml");
    }
}
