package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.engine.GameEngine;
import com.rst.outspelled.engine.SkillCheckResult;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.LetterTile;
import com.rst.outspelled.model.Spell;
import com.rst.outspelled.model.Wizard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameController implements GameEngine.GameListener {

    @FXML private Label player1NameLabel;
    @FXML private Label player2NameLabel;
    @FXML private ProgressBar player1HpBar;
    @FXML private ProgressBar player2HpBar;
    @FXML private Label player1HpLabel;
    @FXML private Label player2HpLabel;
    @FXML private Label timerLabel;
    @FXML private Label turnLabel;
    @FXML private ListView<String> battleLog;
    @FXML private GridPane letterGridPane;
    @FXML private Label selectedWordLabel;
    @FXML private Label feedbackLabel;
    @FXML private Button castButton;
    @FXML private Label skillCheckStatusLabel;
    @FXML private Rectangle wizard1Portrait;
    @FXML private Rectangle wizard2Portrait;

    // Last Stand overlay fields
    @FXML private StackPane lastStandOverlay;
    @FXML private Label scrambledWordLabel;
    @FXML private Label lastStandTimerLabel;
    @FXML private TextField lastStandInput;
    @FXML private Label lastStandFeedbackLabel;

    private static Wizard wizard1;
    private static Wizard wizard2;
    private GameEngine engine;

    private ScheduledExecutorService skillCheckTimerExecutor;
    private int skillCheckSecondsRemaining;

    // tracks whether we are in half hp challenge mode
    private boolean halfHpChallengeActive = false;
    private LetterGrid currentSharedGrid = null;

    public static void setWizards(Wizard w1, Wizard w2) {
        wizard1 = w1;
        wizard2 = w2;
    }

    @FXML
    public void initialize() {
        engine = new GameEngine(wizard1, wizard2, this);

        player1NameLabel.setText(wizard1.getName());
        player2NameLabel.setText(wizard2.getName());

        updateHpDisplay();
        setInputEnabled(false);

        feedbackLabel.setText("Loading dictionary...");
        feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");

        lastStandOverlay.setVisible(false);
        skillCheckStatusLabel.setText("");

        engine.initialize();
    }

    // --- Grid Rendering ---

    private void renderGrid() {
        if (halfHpChallengeActive && currentSharedGrid != null) {
            renderSharedGrid(currentSharedGrid);
        } else {
            renderPlayerGrid(engine.getCurrentGrid());
        }
    }

    private void renderPlayerGrid(LetterGrid grid) {
        letterGridPane.getChildren().clear();
        for (int r = 0; r < grid.getRows(); r++) {
            for (int c = 0; c < grid.getCols(); c++) {
                LetterTile tile = grid.getTile(r, c);
                Button tileButton = createTileButton(tile, r, c, grid);
                letterGridPane.add(tileButton, c, r);
            }
        }
    }

    private void renderSharedGrid(LetterGrid sharedGrid) {
        letterGridPane.getChildren().clear();
        for (int r = 0; r < sharedGrid.getRows(); r++) {
            for (int c = 0; c < sharedGrid.getCols(); c++) {
                LetterTile tile = sharedGrid.getTile(r, c);
                Button tileButton = createTileButton(tile, r, c, sharedGrid);
                letterGridPane.add(tileButton, c, r);
            }
        }
    }

    private Button createTileButton(LetterTile tile, int row, int col,
                                    LetterGrid grid) {
        Button btn = new Button(
                String.valueOf(tile.getLetter()) + "\n" + tile.getValue()
        );
        btn.setPrefSize(62, 62);
        btn.setStyle(tile.isSelected() ? getSelectedStyle() : getIdleStyle());

        btn.setOnAction(e -> {
            if (tile.isSelected()) {
                grid.deselectTile(row, col);
                btn.setStyle(getIdleStyle());
            } else if (tile.isIdle()) {
                grid.selectTile(row, col);
                btn.setStyle(getSelectedStyle());
            }
            updateSelectedWordDisplay();
        });

        return btn;
    }

    private void updateSelectedWordDisplay() {
        LetterGrid grid = halfHpChallengeActive && currentSharedGrid != null
                ? currentSharedGrid
                : engine.getCurrentGrid();
        String word = grid.getSelectedWord();
        selectedWordLabel.setText(word.isEmpty() ? "_ _ _" : word);
    }

    // --- Button Actions ---

    @FXML
    private void onCastClicked() {
        LetterGrid grid = halfHpChallengeActive && currentSharedGrid != null
                ? currentSharedGrid
                : engine.getCurrentGrid();

        String word = grid.getSelectedWord();
        if (word.length() < 3) {
            feedbackLabel.setText("Word must be at least 3 letters!");
            feedbackLabel.setStyle("-fx-text-fill: #ff6b6b;");
            return;
        }

        if (halfHpChallengeActive) {
            // Local: first Cast = player 1, second Cast = player 2 (grid stays enabled for both)
            Wizard toSubmit = engine.getSkillCheckManager().hasSubmittedHalfHp(wizard1) ? wizard2 : wizard1;
            engine.submitHalfHpWord(toSubmit, word);
            feedbackLabel.setText(toSubmit.getName() + " submitted. " +
                    (engine.getSkillCheckManager().hasSubmittedHalfHp(wizard1) && engine.getSkillCheckManager().hasSubmittedHalfHp(wizard2)
                            ? "Resolving..."
                            : "Other player: select letters and Cast!"));
            feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
            grid.deselectAll();
            renderGrid();
            updateSelectedWordDisplay();
            return;
        }

        setInputEnabled(false);
        feedbackLabel.setText("Validating spell...");
        feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
        engine.submitWord(word, grid);
    }

    @FXML
    private void onClearClicked() {
        LetterGrid grid = halfHpChallengeActive && currentSharedGrid != null
                ? currentSharedGrid
                : engine.getCurrentGrid();
        grid.deselectAll();
        renderGrid();
        updateSelectedWordDisplay();
        feedbackLabel.setText("");
    }

    @FXML
    private void onShuffleClicked() {
        if (halfHpChallengeActive) {
            feedbackLabel.setText("Can't shuffle during a skill check!");
            feedbackLabel.setStyle("-fx-text-fill: #ff6b6b;");
            return;
        }
        engine.getCurrentGrid().shuffleGrid();
        renderGrid();
        updateSelectedWordDisplay();
        feedbackLabel.setText("Grid shuffled!");
        feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
    }

    // --- GameEngine.GameListener Implementation ---

    @Override
    public void onGameReady() {
        feedbackLabel.setText("Game started! " + wizard1.getName() + " goes first.");
        feedbackLabel.setStyle("-fx-text-fill: #4caf50;");
        setInputEnabled(true);
        renderGrid();
        updatePortraitHighlight(wizard1);

        letterGridPane.getScene().addEventFilter(
                javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                    if (!letterGridPane.isDisabled()
                            && !lastStandOverlay.isVisible()) {
                        handleKeyInput(event);
                        event.consume();
                    }
                }
        );
    }

    @Override
    public void onSpellCast(Spell spell, Wizard caster, Wizard target) {
        String entry = getSpellEmoji(spell) + " " + spell.getSpellDescription();
        battleLog.getItems().add(0, entry);
        updateHpDisplay();
        renderGrid();

        feedbackLabel.setText(spell.getWord() + " dealt "
                + spell.getTotalDamage() + " damage!");
        feedbackLabel.setStyle("-fx-text-fill: #4caf50;");

        updateHpBarColor(
                caster == wizard1 ? player2HpBar : player1HpBar,
                target.getHpPercentage()
        );

        selectedWordLabel.setText("_ _ _");
    }

    @Override
    public void onInvalidWord(String word) {
        feedbackLabel.setText("\"" + word + "\" is not a valid word. Fix your spell!");
        feedbackLabel.setStyle("-fx-text-fill: #ff6b6b;");
        setInputEnabled(true);
    }

    @Override
    public void onPlayerDefeated(Wizard loser, Wizard winner) {
        stopSkillCheckTimer();
        setInputEnabled(false);
        feedbackLabel.setText(winner.getName() + " wins the duel!");
        feedbackLabel.setStyle("-fx-text-fill: #e2b96f;");
        battleLog.getItems().add(0, "🏆 " + winner.getName()
                + " has defeated " + loser.getName() + "!");
        showGameOverDialog(winner, loser);
    }

    @Override
    public void onTurnChanged(Wizard currentPlayer) {
        halfHpChallengeActive = false;
        currentSharedGrid = null;
        turnLabel.setText(currentPlayer.getName() + "'s Turn");
        timerLabel.setStyle(
                "-fx-text-fill: #e2b96f; -fx-font-size: 30px; -fx-font-weight: bold;");
        setInputEnabled(true);
        renderGrid();
        updatePortraitHighlight(currentPlayer);
        selectedWordLabel.setText("_ _ _");
        skillCheckStatusLabel.setText("");
        feedbackLabel.setText(currentPlayer.getName()
                + "'s turn — select your letters!");
        feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
    }

    @Override
    public void onTimerTick(int secondsRemaining) {
        timerLabel.setText(String.valueOf(secondsRemaining));
        if (secondsRemaining <= 10) {
            timerLabel.setStyle("-fx-text-fill: #ff6b6b;" +
                    "-fx-font-size: 30px; -fx-font-weight: bold;");
        } else {
            timerLabel.setStyle("-fx-text-fill: #e2b96f;" +
                    "-fx-font-size: 30px; -fx-font-weight: bold;");
        }
    }

    @Override
    public void onTimeExpired(Wizard currentPlayer) {
        feedbackLabel.setText(currentPlayer.getName()
                + " ran out of time! Turn forfeited.");
        feedbackLabel.setStyle("-fx-text-fill: #ff6b6b;");
        battleLog.getItems().add(0, "⌛ " + currentPlayer.getName()
                + " ran out of time!");
        setInputEnabled(false);
    }

    // --- Skill Check Listener Implementation ---

    @Override
    public void onHalfHpPrompt(Wizard initiator) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Skill Check Available!");
        alert.setHeaderText(initiator.getName() + " can initiate a challenge!");
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
            if (response == initiate) {
                engine.initiateHalfHpChallenge(initiator);
            } else {
                engine.skipHalfHpChallenge();
            }
        });
    }

    @Override
    public void onHalfHpChallengeStart(LetterGrid sharedGrid) {
        halfHpChallengeActive = true;
        currentSharedGrid = sharedGrid;

        skillCheckStatusLabel.setText(
                "⚔ HALF HP CHALLENGE — Both players spell a word!");
        skillCheckStatusLabel.setStyle("-fx-text-fill: #ff6b6b;");
        turnLabel.setText("⚔ SKILL CHECK");
        timerLabel.setStyle(
                "-fx-text-fill: #ff6b6b; -fx-font-size: 30px; -fx-font-weight: bold;");

        renderSharedGrid(sharedGrid);
        selectedWordLabel.setText("_ _ _");
        feedbackLabel.setText("Select letters and cast your best spell!");
        feedbackLabel.setStyle("-fx-text-fill: #e2b96f;");
        setInputEnabled(true);

        startSkillCheckTimer(() -> engine.forceResolveHalfHp());
    }

    @Override
    public void onHalfHpChallengeComplete(SkillCheckResult result) {
        stopSkillCheckTimer();
        halfHpChallengeActive = false;
        currentSharedGrid = null;

        String summary = result.getSummary();
        skillCheckStatusLabel.setText(summary);
        skillCheckStatusLabel.setStyle("-fx-text-fill: #e2b96f;");
        battleLog.getItems().add(0, "⚔ SKILL CHECK: " + summary);
        updateHpDisplay();
        updateHpBarColor(player1HpBar, wizard1.getHpPercentage());
        updateHpBarColor(player2HpBar, wizard2.getHpPercentage());

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    skillCheckStatusLabel.setText("");
                    renderGrid();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void onLastStandStart(String scrambledWord) {
        lastStandOverlay.setVisible(true);
        scrambledWordLabel.setText(scrambledWord);
        lastStandFeedbackLabel.setText("");
        lastStandInput.clear();
        lastStandInput.setDisable(false);

        // small delay then focus input
        Platform.runLater(() -> lastStandInput.requestFocus());

        lastStandInput.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                String attempt = lastStandInput.getText().trim();
                if (!attempt.isEmpty()) {
                    engine.submitLastStandWord(engine.getCurrentPlayer(), attempt);
                    lastStandFeedbackLabel.setText("Answer submitted! Waiting...");
                    lastStandFeedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
                    lastStandInput.setDisable(true);
                }
            }
        });

        startSkillCheckTimer(() -> engine.forceResolveLastStand());
    }

    @Override
    public void onLastStandComplete(SkillCheckResult result) {
        stopSkillCheckTimer();
        lastStandOverlay.setVisible(false);
        lastStandInput.setDisable(false);

        String summary = result.getSummary();
        battleLog.getItems().add(0, "⚡ LAST STAND: " + summary);
        updateHpDisplay();
        updateHpBarColor(player1HpBar, wizard1.getHpPercentage());
        updateHpBarColor(player2HpBar, wizard2.getHpPercentage());

        skillCheckStatusLabel.setText(summary);
        skillCheckStatusLabel.setStyle("-fx-text-fill: #e2b96f;");

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> skillCheckStatusLabel.setText(""));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // --- Skill Check Timer ---

    private void startSkillCheckTimer(Runnable onExpire) {
        skillCheckSecondsRemaining =
                engine.getSkillCheckManager().getSkillCheckDuration();

        skillCheckTimerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("SkillCheckTimerThread");
            t.setDaemon(true);
            return t;
        });

        skillCheckTimerExecutor.scheduleAtFixedRate(() -> {
            skillCheckSecondsRemaining--;
            Platform.runLater(() -> {
                timerLabel.setText(String.valueOf(skillCheckSecondsRemaining));
                lastStandTimerLabel.setText(
                        String.valueOf(skillCheckSecondsRemaining));
                if (skillCheckSecondsRemaining <= 5) {
                    timerLabel.setStyle("-fx-text-fill: #ff6b6b;" +
                            "-fx-font-size: 30px; -fx-font-weight: bold;");
                }
                if (skillCheckSecondsRemaining <= 0) {
                    stopSkillCheckTimer();
                    onExpire.run();
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopSkillCheckTimer() {
        if (skillCheckTimerExecutor != null) {
            skillCheckTimerExecutor.shutdown();
            skillCheckTimerExecutor = null;
        }
    }

    // --- Helpers ---
    private void updatePortraitHighlight(Wizard currentPlayer) {
        if (wizard1Portrait == null || wizard2Portrait == null) return;

        final double activeStrokeWidth = 6;
        final double inactiveStrokeWidth = 1.5;
        final String activeColor = "#e2b96f";   // gold — current player
        final String inactiveColor = "#444466"; // dim border — other player

        if (currentPlayer == wizard1) {
            wizard1Portrait.setStroke(Color.web(activeColor));
            wizard1Portrait.setStrokeWidth(activeStrokeWidth);
            wizard2Portrait.setStroke(Color.web(inactiveColor));
            wizard2Portrait.setStrokeWidth(inactiveStrokeWidth);
        } else {
            wizard2Portrait.setStroke(Color.web(activeColor));
            wizard2Portrait.setStrokeWidth(activeStrokeWidth);
            wizard1Portrait.setStroke(Color.web(inactiveColor));
            wizard1Portrait.setStrokeWidth(inactiveStrokeWidth);
        }
    }

    private void updateHpDisplay() {
        player1HpBar.setProgress(wizard1.getHpPercentage());
        player2HpBar.setProgress(wizard2.getHpPercentage());
        player1HpLabel.setText(wizard1.getHp() + " / " + wizard1.getMaxHp());
        player2HpLabel.setText(wizard2.getHp() + " / " + wizard2.getMaxHp());
    }

    private void updateHpBarColor(ProgressBar bar, double percentage) {
        if (percentage > 0.5) {
            bar.setStyle("-fx-accent: #4caf50;");
        } else if (percentage > 0.25) {
            bar.setStyle("-fx-accent: #ff9800;");
        } else {
            bar.setStyle("-fx-accent: #f44336;");
        }
    }

    private void setInputEnabled(boolean enabled) {
        castButton.setDisable(!enabled);
        letterGridPane.setDisable(!enabled);
    }

    private String getIdleStyle() {
        return "-fx-background-color: #2a2a4a;" +
                "-fx-text-fill: #e2b96f;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: #444466;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;";
    }

    private String getSelectedStyle() {
        return "-fx-background-color: #e2b96f;" +
                "-fx-text-fill: #1a1a2e;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: #ffffff;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-cursor: hand;";
    }

    private String getSpellEmoji(Spell spell) {
        return switch (spell.getPower()) {
            case WEAK -> "🔹";
            case MODERATE -> "🔷";
            case STRONG -> "💥";
            case DEVASTATING -> "⚡";
        };
    }

    private void showGameOverDialog(Wizard winner, Wizard loser) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Duel Over!");
        alert.setHeaderText(winner.getName() + " wins!");
        alert.setContentText(winner.getName() + " defeated "
                + loser.getName() + "!\n\nPlay again?");

        ButtonType playAgain = new ButtonType("Play Again");
        ButtonType mainMenu = new ButtonType("Main Menu");
        alert.getButtonTypes().setAll(playAgain, mainMenu);

        alert.showAndWait().ifPresent(response -> {
            engine.shutdown();
            if (response == playAgain) {
                GameController.setWizards(
                        new Wizard(wizard1.getName(), 200,
                                wizard1.getSkin(), wizard1.getPreferredArena()),
                        new Wizard(wizard2.getName(), 200,
                                wizard2.getSkin(), wizard2.getPreferredArena())
                );
                Main.navigateTo("game-view.fxml");
            } else {
                Main.navigateTo("menu-view.fxml");
            }
        });
    }

    private void handleKeyInput(javafx.scene.input.KeyEvent event) {
        String key = event.getText();
        javafx.scene.input.KeyCode code = event.getCode();

        LetterGrid grid = halfHpChallengeActive && currentSharedGrid != null
                ? currentSharedGrid
                : engine.getCurrentGrid();

        if (code == javafx.scene.input.KeyCode.BACK_SPACE) {
            String currentWord = grid.getSelectedWord();
            if (!currentWord.isEmpty()) {
                char lastChar = currentWord.charAt(currentWord.length() - 1);
                grid.deselectLastMatchingTile(lastChar);
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
            onShuffleClicked();
            return;
        }

        if (key != null && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            LetterTile matched = grid.selectFirstMatchingTile(key.charAt(0));
            if (matched != null) {
                renderGrid();
                updateSelectedWordDisplay();
            } else {
                feedbackLabel.setText("Letter '"
                        + key.toUpperCase() + "' not available!");
                feedbackLabel.setStyle("-fx-text-fill: #ff6b6b;");
            }
        }
    }
}