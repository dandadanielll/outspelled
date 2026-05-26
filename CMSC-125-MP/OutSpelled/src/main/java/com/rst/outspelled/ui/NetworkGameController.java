package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.LetterTile;
import com.rst.outspelled.model.Spell;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.network.GameClient;
import com.rst.outspelled.network.SessionManager;
import com.rst.outspelled.util.SoundManager;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Game view for LAN: same letter-grid gameplay as local; state from server, words sent to server.
 */
public class NetworkGameController {

    @FXML private Label player1NameLabel;
    @FXML private Label player2NameLabel;
    @FXML private javafx.scene.layout.VBox player1Hearts;
    @FXML private javafx.scene.layout.VBox player2Hearts;
    @FXML private Label player1HpLabel;
    @FXML private Label player2HpLabel;
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
    private static final int HEARTS_COUNT = 20;
    private static final int HP_PER_HEART = 10;
    private static final int SKILL_CHECK_SEC = 15;

    private LetterGrid letterGrid;
    private final List<Label> player1HeartLabels = new ArrayList<>();
    private final List<Label> player2HeartLabels = new ArrayList<>();
    private boolean halfHpChallengeActive = false;
    private ScheduledExecutorService skillCheckTimer;
    private volatile int lastStandSecondsLeft = SKILL_CHECK_SEC;
    private String opponentTypingWord = "";

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
        SoundManager.startBgm("BattleMusic.wav"); // switch to battle music
    }

    public static void applyShuffleGrid(int shufflerId, String letters) {
        Platform.runLater(() -> {
            if (instance != null && instance.letterGrid != null && letters != null && letters.length() >= 16) {
                if (shufflerId == myPlayerId) {
                    instance.letterGrid.applyLayoutIdleOnly(letters);
                    instance.renderGrid();
                    instance.updateSelectedWordDisplay();
                    instance.feedbackLabel.setText("Grid shuffled!");
                    instance.feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
                } else {
                    instance.letterGrid.applyLayout(letters);
                    instance.renderGrid();
                    instance.updateSelectedWordDisplay();
                    instance.feedbackLabel.setText("Opponent shuffled grid!");
                    instance.feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
                }
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
                SoundManager.playKeyDelete(); // delete key sound
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
                SoundManager.playKeyTap(); // key tap for each letter selected
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
        
        Map<Character, Integer> opponentLetters = new HashMap<>();
        if (!opponentTypingWord.isEmpty()) {
            for (char c : opponentTypingWord.toCharArray()) {
                opponentLetters.put(c, opponentLetters.getOrDefault(c, 0) + 1);
            }
        }

        for (int r = 0; r < letterGrid.getRows(); r++) {
            for (int c = 0; c < letterGrid.getCols(); c++) {
                LetterTile tile = letterGrid.getTile(r, c);
                boolean isOpponentSelected = false;
                
                if (!opponentTypingWord.isEmpty() && opponentLetters.getOrDefault(tile.getLetter(), 0) > 0) {
                    isOpponentSelected = true;
                    opponentLetters.put(tile.getLetter(), opponentLetters.get(tile.getLetter()) - 1);
                }
                
                Button btn = createTileButton(tile, r, c, isOpponentSelected);
                letterGridPane.add(btn, c, r);
            }
        }
    }

    private Button createTileButton(LetterTile tile, int row, int col, boolean isOpponentSelected) {
        Button btn = new Button();
        btn.setPrefSize(62, 62);
        btn.setMinSize(62, 62);
        btn.setMaxSize(62, 62);

        StackPane graphicPane = new StackPane();
        graphicPane.setPrefSize(58, 58);
        graphicPane.setMinSize(58, 58);
        graphicPane.setMaxSize(58, 58);

        Label letterLabel = new Label(String.valueOf(tile.getLetter()));
        letterLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label valueLabel = new Label(String.valueOf(tile.getValue()));
        valueLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 9px; -fx-font-weight: bold;");

        graphicPane.getChildren().addAll(letterLabel, valueLabel);
        StackPane.setAlignment(letterLabel, javafx.geometry.Pos.CENTER);
        StackPane.setAlignment(valueLabel, javafx.geometry.Pos.BOTTOM_RIGHT);
        valueLabel.setTranslateX(-2);
        valueLabel.setTranslateY(-1);

        btn.setGraphic(graphicPane);

        java.util.function.Consumer<String> updateColors = (state) -> {
            if ("selected".equals(state)) {
                btn.setStyle(getSelectedStyle());
                letterLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1000;");
                valueLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: rgba(26, 16, 0, 0.65);");
            } else if ("opponentSelected".equals(state)) {
                btn.setStyle(getOpponentSelectedStyle());
                letterLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e0d0f0;");
                valueLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: rgba(224, 208, 240, 0.65);");
            } else if ("hover".equals(state)) {
                btn.setStyle(getHoverStyle());
                letterLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
                valueLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: rgba(255, 255, 255, 0.65);");
            } else {
                btn.setStyle(getIdleStyle());
                letterLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e2b96f;");
                valueLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: rgba(226, 185, 111, 0.65);");
            }
        };

        if (tile.isSelected()) {
            updateColors.accept("selected");
        } else if (isOpponentSelected) {
            updateColors.accept("opponentSelected");
        } else {
            updateColors.accept("idle");
        }

        btn.setOnMouseEntered(e -> {
            if (!tile.isSelected() && !isOpponentSelected && !btn.isDisabled()) {
                updateColors.accept("hover");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) {
                if (tile.isSelected()) {
                    updateColors.accept("selected");
                } else if (isOpponentSelected) {
                    updateColors.accept("opponentSelected");
                } else {
                    updateColors.accept("idle");
                }
            }
        });

        btn.setOnAction(e -> {
            if (tile.isSelected()) {
                letterGrid.deselectTile(row, col);
                updateColors.accept(btn.isHover() ? "hover" : "idle");
            } else if (tile.isIdle() && !isOpponentSelected) {
                letterGrid.selectTile(row, col);
                updateColors.accept("selected");
            }
            updateSelectedWordDisplay();
        });
        return btn;
    }

    private static String getIdleStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #424266 0%, #424266 3px, #2a2a4a 3px, #1a1a30 100%);"
                + "-fx-background-insets: 0;"
                + "-fx-background-radius: 0;"
                + "-fx-border-color: #555588 #1a1a2a #1a1a2a #555588;"
                + "-fx-border-width: 2px;"
                + "-fx-border-radius: 0;"
                + "-fx-text-fill: #e2b96f;"
                + "-fx-font-family: 'Pixelify Sans';"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.55), 0, 0.0, 2, 2);";
    }

    private static String getHoverStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #50507d 0%, #50507d 3px, #33335c 3px, #202042 100%);"
                + "-fx-background-insets: 0;"
                + "-fx-background-radius: 0;"
                + "-fx-border-color: #6e6eab #222238 #222238 #6e6eab;"
                + "-fx-border-width: 2px;"
                + "-fx-border-radius: 0;"
                + "-fx-text-fill: #ffffff;"
                + "-fx-font-family: 'Pixelify Sans';"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(110,110,180,0.18), 6, 0.1, 0, 0);";
    }

    private static String getSelectedStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #ffe9a0 0%, #ffe9a0 3px, #f0c040 3px, #e8a828 100%);"
                + "-fx-background-insets: 0;"
                + "-fx-background-radius: 0;"
                + "-fx-border-color: #fff5c0 #c09030 #c09030 #fff5c0;"
                + "-fx-border-width: 2px;"
                + "-fx-border-radius: 0;"
                + "-fx-text-fill: #1a1000;"
                + "-fx-font-family: 'Pixelify Sans';"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(255,200,60,0.22), 6, 0.1, 0, 0);";
    }

    private static String getOpponentSelectedStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #8060a0 0%, #8060a0 3px, #5a3a7a 3px, #3a1a5a 100%);"
                + "-fx-background-insets: 0;"
                + "-fx-background-radius: 0;"
                + "-fx-border-color: #a080c0 #2a1040 #2a1040 #a080c0;"
                + "-fx-border-width: 2px;"
                + "-fx-border-radius: 0;"
                + "-fx-text-fill: #e0d0f0;"
                + "-fx-font-family: 'Pixelify Sans';"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: default;"
                + "-fx-effect: dropshadow(three-pass-box, rgba(160,80,200,0.25), 6, 0.1, 0, 0);";
    }

    private void buildHearts() {
        if (player1Hearts == null || player2Hearts == null) return;
        player1Hearts.getChildren().clear();
        player2Hearts.getChildren().clear();
        player1HeartLabels.clear();
        player2HeartLabels.clear();
        
        HBox p1Row1 = new HBox(2); p1Row1.setAlignment(javafx.geometry.Pos.CENTER);
        HBox p1Row2 = new HBox(2); p1Row2.setAlignment(javafx.geometry.Pos.CENTER);
        HBox p2Row1 = new HBox(2); p2Row1.setAlignment(javafx.geometry.Pos.CENTER);
        HBox p2Row2 = new HBox(2); p2Row2.setAlignment(javafx.geometry.Pos.CENTER);

        for (int i = 0; i < HEARTS_COUNT; i++) {
            Label h1 = new Label("♥");
            h1.setStyle("-fx-text-fill: #e24a4a; -fx-font-size: 18px;");
            (i < 10 ? p1Row1 : p1Row2).getChildren().add(h1);
            player1HeartLabels.add(h1);
            
            Label h2 = new Label("♥");
            h2.setStyle("-fx-text-fill: #e24a4a; -fx-font-size: 18px;");
            (i < 10 ? p2Row1 : p2Row2).getChildren().add(h2);
            player2HeartLabels.add(h2);
        }
        
        player1Hearts.getChildren().addAll(p1Row1, p1Row2);
        player2Hearts.getChildren().addAll(p2Row1, p2Row2);
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
        if (player1HpLabel != null) player1HpLabel.setText(p1Hp + " / " + MAX_HP);
        if (player2HpLabel != null) player2HpLabel.setText(p2Hp + " / " + MAX_HP);
    }

    private void updateSelectedWordDisplay() {
        String word = letterGrid.getSelectedWord();
        updateFloatingLetters(word);
        updateDamagePreview(word);
        if (client != null && currentTurn == myPlayerId && !halfHpChallengeActive) {
            client.sendTyping(word);
        }
    }

    private void updateFloatingLetters(String word) {
        if (floatingLettersPane == null) return;
        floatingLettersPane.getChildren().clear();
        if (word == null || word.isEmpty()) return;
        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);
            int points = 1;
            if (letterGrid != null) {
                for (int r = 0; r < letterGrid.getRows(); r++) {
                    for (int c = 0; c < letterGrid.getCols(); c++) {
                        LetterTile t = letterGrid.getTile(r, c);
                        if (t.getLetter() == letter) {
                            points = t.getValue();
                            break;
                        }
                    }
                }
            }

            StackPane tile = new StackPane();
            tile.setPrefSize(56, 62);
            tile.setMinSize(56, 62);
            tile.setMaxSize(56, 62);
            tile.setStyle("-fx-background-color: linear-gradient(to bottom, #ffe9a0 0%, #ffe9a0 3px, #f0c040 3px, #e8a828 100%); " +
                          "-fx-background-insets: 0; " +
                          "-fx-background-radius: 0; " +
                          "-fx-border-color: #fff5c0 #c09030 #c09030 #fff5c0; " +
                          "-fx-border-width: 2px; " +
                          "-fx-border-radius: 0; " +
                          "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.35), 0, 0.0, 2, 2);");

            Label letterLbl = new Label(String.valueOf(letter));
            letterLbl.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a1000;");

            Label valueLbl = new Label(String.valueOf(points));
            valueLbl.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: rgba(26, 16, 0, 0.65);");

            tile.getChildren().addAll(letterLbl, valueLbl);
            StackPane.setAlignment(letterLbl, javafx.geometry.Pos.CENTER);
            StackPane.setAlignment(valueLbl, javafx.geometry.Pos.BOTTOM_RIGHT);
            valueLbl.setTranslateX(-3);
            valueLbl.setTranslateY(-2);

            floatingLettersPane.getChildren().add(tile);
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
        
        int oppHp = myPlayerId == 1 ? currentP2Hp : currentP1Hp;
        int oppFullHearts = (int) Math.ceil(oppHp / (double) HP_PER_HEART);
        int heartsToBlink = (int) Math.ceil(damage / (double) HP_PER_HEART);
        
        int startIdx = Math.max(0, oppFullHearts - heartsToBlink);
        int endIdx = oppFullHearts; // Only blink up to what they actually have
        
        List<Label> toBlink = (myPlayerId == 1) ? player2HeartLabels : player1HeartLabels;
        
        final String fullStyle = "-fx-text-fill: #e24a4a; -fx-font-size: 18px;";
        final String dimStyle = "-fx-text-fill: #442020; -fx-font-size: 18px;";
        
        blinkTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(0), e -> {
                    for (int i = startIdx; i < endIdx; i++) {
                        if (i < toBlink.size()) toBlink.get(i).setStyle("-fx-text-fill: #ff8888; -fx-font-size: 18px;");
                    }
                }),
                new javafx.animation.KeyFrame(Duration.millis(400), e -> {
                    for (int i = startIdx; i < endIdx; i++) {
                        if (i < toBlink.size()) toBlink.get(i).setStyle(i < oppFullHearts ? fullStyle : dimStyle);
                    }
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
        if (currentTurn == 1) {
            wizard1Portrait.setOpacity(1.0);
            wizard2Portrait.setOpacity(0.5);
        } else {
            wizard2Portrait.setOpacity(1.0);
            wizard1Portrait.setOpacity(0.5);
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
                SoundManager.playInvalid(); // key_delete sound for invalid word
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
                instance.opponentTypingWord = "";
                instance.updateFloatingLetters("");
                instance.renderGrid();
                if (instance.feedbackLabel != null) {
                    instance.feedbackLabel.setText("");
                }
                instance.turnLabel.setText(instance.currentTurnName() + "'s Turn");
                instance.updateInputEnabled();
                instance.updatePortraitHighlight();
                if (instance.skillCheckStatusLabel != null) {
                    instance.skillCheckStatusLabel.setText(""); // clear previous challenge result
                }
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
            
            javafx.scene.control.Label headerLabel = com.rst.outspelled.util.DialogBuilder.styledDialogLabel(name + " can initiate a challenge!");
            javafx.scene.control.Label contentLabel = new javafx.scene.control.Label(
                    "You are at or below 50% HP.\n\n" +
                            "Initiate the Half HP Challenge?\n" +
                            "Win → opponent drops to your HP level\n" +
                            "Lose → you take word damage\n\n" +
                            "This can only be used once per match!"
            );
            contentLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 13px; -fx-wrap-text: true; -fx-text-alignment: center;");
            contentLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            javafx.scene.layout.StackPane initiateBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("⚔ Initiate!", true);
            initiateBtn.setOnMouseClicked(e -> {
                SoundManager.playClick();
                com.rst.outspelled.util.OverlayManager.hideOverlay();
                client.sendHalfHpInitiate();
            });

            javafx.scene.layout.StackPane skipBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("Skip", false);
            skipBtn.setOnMouseClicked(e -> {
                SoundManager.playClick();
                com.rst.outspelled.util.OverlayManager.hideOverlay();
                client.sendHalfHpSkip();
            });

            javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(15, skipBtn, initiateBtn);
            btnBox.setAlignment(javafx.geometry.Pos.CENTER);

            javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(15);
            body.setStyle("-fx-padding: 20 24 24 24;");
            body.setAlignment(javafx.geometry.Pos.CENTER);
            body.getChildren().addAll(headerLabel, contentLabel, btnBox);

            javafx.scene.layout.VBox dialogRoot = com.rst.outspelled.util.DialogBuilder.buildDialogRoot("Skill Check Available!", true, body, 420, 320);
            com.rst.outspelled.util.OverlayManager.showOverlay(dialogRoot);
        });
    }

    public static void onHalfHpStart(long gridSeed) {
        Platform.runLater(() -> {
            if (instance != null && instance.letterGrid != null) {
                SoundManager.playSkillCheck();
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
                String summary;
                if (initiatorWon == 1) {
                    summary = "Skill check: Initiator wins! Opponent takes damage.";
                } else if (initiatorWon == 2) {
                    summary = "Skill check: It's a tie! No damage dealt.";
                } else {
                    summary = "Skill check: Initiator loses! They take " + damageOrHeal + " damage.";
                }
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
                SoundManager.playSkillCheck();
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
                // Stop battle music then play the appropriate fanfare
                SoundManager.stopBgm();
                if (winnerId == myPlayerId) {
                    SoundManager.playVictory(); // this client won
                } else {
                    SoundManager.playDefeat();  // this client lost
                }

                String winnerName = winnerId == 1 ? (wizard1 != null ? wizard1.getName() : "Player 1")
                        : (wizard2 != null ? wizard2.getName() : "Player 2");
                instance.feedbackLabel.setText(winnerName + " wins!");
                instance.feedbackLabel.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 16px;");
                instance.castButton.setDisable(true);
                instance.letterGridPane.setDisable(true);
                if (instance.menuButton != null) instance.menuButton.setDisable(false);
                
                javafx.scene.control.Label headerLabel = com.rst.outspelled.util.DialogBuilder.styledDialogLabel(winnerName + " wins!");
                javafx.scene.layout.StackPane menuBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("Main Menu", true);
                menuBtn.setOnMouseClicked(e -> {
                    SoundManager.playClick();
                    com.rst.outspelled.util.OverlayManager.hideOverlay();
                    Main.navigateTo("menu-view.fxml");
                });
                javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(20);
                body.setStyle("-fx-padding: 30 24 24 24;");
                body.setAlignment(javafx.geometry.Pos.CENTER);
                body.getChildren().addAll(headerLabel, menuBtn);
                javafx.scene.layout.VBox dialogRoot = com.rst.outspelled.util.DialogBuilder.buildDialogRoot("🏆 Game Over 🏆", false, body, 400, 250);
                com.rst.outspelled.util.OverlayManager.showOverlay(dialogRoot);
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
                SoundManager.playCast(); // cast sound for skill check word too
                client.sendHalfHpWord(word);
                letterGrid.deselectAll();
                renderGrid();
                updateSelectedWordDisplay();
                feedbackLabel.setText("Word submitted! Waiting for opponent...");
                feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
                castButton.setDisable(true);
                return;
            }
            SoundManager.playCast(); // cast sound on word submission
            client.sendWord(word);

            renderGrid();
            updateSelectedWordDisplay();
            feedbackLabel.setText("Casting...");
            feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
            castButton.setDisable(true);
            letterGridPane.setDisable(true);
            client.sendTyping(""); // Clear opponent's typing view
        }
    }

    @FXML
    private void onClearClicked() {
        letterGrid.deselectAll();
        renderGrid();
        updateSelectedWordDisplay();
        feedbackLabel.setText("");
    }

    public static void onOpponentTyping(String word) {
        if (instance != null && currentTurn != myPlayerId && !instance.halfHpChallengeActive) {
            instance.opponentTypingWord = word != null ? word.toUpperCase() : "";
            if (word == null || word.isEmpty()) {
                instance.feedbackLabel.setText("Waiting for opponent...");
                instance.feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
            } else {
                instance.feedbackLabel.setText("Opponent is typing: " + word.toUpperCase());
                instance.feedbackLabel.setStyle("-fx-text-fill: #e2b96f;");
            }
            instance.updateFloatingLetters(instance.opponentTypingWord);
            instance.renderGrid();
        }
    }

    @FXML
    private void onShuffleClicked() {
        if (shuffleButton != null && shuffleButton.isDisabled()) return;
        letterGrid.shuffleIdleTilesOnly();
        if (client != null) {
            client.sendShuffle(letterGrid.getLettersAsString());
        }
        renderGrid();
        updateSelectedWordDisplay();
        feedbackLabel.setText("Grid shuffled!");
        feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
    }

    @FXML
    private void onOptionsClicked() {
        SoundManager.playClick();
        javafx.scene.control.Label headerLabel = com.rst.outspelled.util.DialogBuilder.styledDialogLabel("Adjust Game Volumes");
        headerLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-text-fill: #8899aa;");

        javafx.scene.control.Label bgmLabel = new javafx.scene.control.Label("Background Music");
        bgmLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 14px; -fx-text-fill: #e2b96f;");
        javafx.scene.control.Slider bgmSlider = new javafx.scene.control.Slider(0, 1.0, SoundManager.getBgmVolume());
        bgmSlider.valueProperty().addListener((obs, oldVal, newVal) -> SoundManager.setBgmVolume(newVal.doubleValue()));

        javafx.scene.control.Label sfxLabel = new javafx.scene.control.Label("Sound Effects");
        sfxLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 14px; -fx-text-fill: #e2b96f;");
        javafx.scene.control.Slider sfxSlider = new javafx.scene.control.Slider(0, 1.0, SoundManager.getSfxVolume());
        sfxSlider.valueProperty().addListener((obs, oldVal, newVal) -> SoundManager.setSfxVolume(newVal.doubleValue()));
        sfxSlider.setOnMouseReleased(e -> SoundManager.playClick());

        javafx.scene.layout.VBox bgmBox = new javafx.scene.layout.VBox(5, bgmLabel, bgmSlider);
        javafx.scene.layout.VBox sfxBox = new javafx.scene.layout.VBox(5, sfxLabel, sfxSlider);
        bgmBox.setAlignment(javafx.geometry.Pos.CENTER);
        sfxBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.StackPane closeBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("Resume", true);
        closeBtn.setOnMouseClicked(e -> {
            SoundManager.playClick();
            com.rst.outspelled.util.OverlayManager.hideOverlay();
        });

        javafx.scene.layout.StackPane quitBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("Quit to Menu", false);
        quitBtn.setOnMouseClicked(e -> {
            SoundManager.playClick();
            com.rst.outspelled.util.OverlayManager.hideOverlay();
            if (client != null) {
                client.sendDisconnect();
            }
            SessionManager.clear();
            Main.navigateTo("menu-view.fxml");
        });
        
        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(10, closeBtn, quitBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(24);
        body.setStyle("-fx-padding: 30 24 24 24;");
        body.setAlignment(javafx.geometry.Pos.CENTER);
        body.getChildren().addAll(headerLabel, bgmBox, sfxBox, buttons);

        javafx.scene.layout.VBox dialogRoot = com.rst.outspelled.util.DialogBuilder.buildDialogRoot("⚙  Options  ⚙", false, body, 400, 380);
        com.rst.outspelled.util.OverlayManager.showOverlay(dialogRoot);
    }
}
