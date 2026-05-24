package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.engine.GameEngine;
import com.rst.outspelled.engine.SkillCheckResult;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.LetterTile;
import com.rst.outspelled.model.Spell;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.util.SoundManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.animation.Transition;
import javafx.geometry.Rectangle2D;
import javafx.util.Duration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameController implements GameEngine.GameListener {

    @FXML
    protected BorderPane gameRoot;

    @FXML
    protected Label player1NameLabel;
    @FXML
    protected Label player2NameLabel;
    @FXML
    protected ProgressBar player1HpBar;
    @FXML
    protected ProgressBar player2HpBar;
    @FXML
    protected Label player1HpLabel;
    @FXML
    protected Label player2HpLabel;
    @FXML
    protected Label timerLabel;
    @FXML
    protected Label turnLabel;
    @FXML
    protected ListView<String> battleLog;
    @FXML
    protected GridPane letterGridPane;
    @FXML
    protected Label selectedWordLabel;
    @FXML
    protected Label feedbackLabel;
    @FXML
    protected Button castButton;
    @FXML
    protected Label skillCheckStatusLabel;
    @FXML
    protected StackPane wizard1Portrait;
    @FXML
    protected StackPane wizard2Portrait;
    @FXML
    protected Label wizard1NameTag;
    @FXML
    protected Label wizard2NameTag;

    // Last Stand overlay fields
    @FXML
    protected StackPane lastStandOverlay;
    @FXML
    protected Label scrambledWordLabel;
    @FXML
    protected Label lastStandTimerLabel;
    @FXML
    protected TextField lastStandInput;
    @FXML
    protected Label lastStandFeedbackLabel;

    protected static Wizard wizard1;
    protected static Wizard wizard2;
    protected GameEngine engine;

    protected CustomSpriteTransition wizard1Animation;
    protected CustomSpriteTransition wizard2Animation;

    protected ScheduledExecutorService skillCheckTimerExecutor;
    protected int skillCheckSecondsRemaining;

    // tracks whether we are in half hp challenge mode
    protected boolean halfHpChallengeActive = false;
    protected LetterGrid currentSharedGrid = null;

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

        // Set background to Game-BG.png in assets
        java.net.URL bgUrl = GameController.class.getResource("/assets/BG-platform.png");
        if (bgUrl != null) {
            gameRoot.setStyle("-fx-background-color: #1a1a2e; " +
                    "-fx-background-image: url('" + bgUrl.toExternalForm() + "'); " +
                    "-fx-background-size: 100% 100%; " +
                    "-fx-background-repeat: no-repeat; " +
                    "-fx-background-position: center;");
        }

        // Load wizard skin portraits
        loadWizardPortrait(wizard1Portrait, wizard1);
        loadWizardPortrait(wizard2Portrait, wizard2);

        engine.initialize();
    }

    /**
     * Fills a portrait StackPane with the wizard's skin image, clipped to rounded
     * corners and animated if a sprite sheet.
     */
    private void loadWizardPortrait(StackPane pane, Wizard wizard) {
        if (pane == null || wizard == null)
            return;
        String imageName = wizard.getSkin().getImagePath();
        java.net.URL imgUrl = GameController.class.getResource("/assets/" + imageName);
        if (imgUrl == null)
            return;
        try {
            Image img = new Image(imgUrl.toExternalForm());
            ImageView iv = new ImageView(img);
            iv.setFitWidth(120);
            iv.setFitHeight(120);
            iv.setPreserveRatio(true);
            iv.setSmooth(false);
            iv.setViewport(new Rectangle2D(0, 0, 128, 128));

            // Rounded clip to match the border-radius
            Rectangle clip = new Rectangle(120, 120);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            iv.setClip(clip);

            CustomSpriteTransition animation = new CustomSpriteTransition(
                    iv, Duration.millis(800), 2, 2, 128.0, 128.0);
            animation.setCycleCount(Transition.INDEFINITE);
            animation.play();

            if (pane == wizard1Portrait) {
                if (wizard1Animation != null) {
                    wizard1Animation.stop();
                }
                wizard1Animation = animation;
            } else if (pane == wizard2Portrait) {
                if (wizard2Animation != null) {
                    wizard2Animation.stop();
                }
                wizard2Animation = animation;
            }

            pane.getChildren().setAll(iv);
        } catch (Exception ignored) {
            // image stays as empty styled pane on failure
        }
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

        updateColors.accept(tile.isSelected() ? "selected" : "idle");

        btn.setOnMouseEntered(e -> {
            if (!tile.isSelected() && !btn.isDisabled()) {
                updateColors.accept("hover");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.isDisabled()) {
                updateColors.accept(tile.isSelected() ? "selected" : "idle");
            }
        });

        btn.setOnAction(e -> {
            if (tile.isSelected()) {
                grid.deselectTile(row, col);
                updateColors.accept(btn.isHover() ? "hover" : "idle");
            } else if (tile.isIdle()) {
                grid.selectTile(row, col);
                updateColors.accept("selected");
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
        
        if (word.isEmpty()) {
            selectedWordLabel.setText("");
            selectedWordLabel.setGraphic(null);
        } else {
            javafx.scene.layout.HBox tiles = new javafx.scene.layout.HBox(4);
            tiles.setAlignment(javafx.geometry.Pos.CENTER);
            
            for (int i = 0; i < word.length(); i++) {
                char letter = word.charAt(i);
                int points = 1;
                for (int r = 0; r < grid.getRows(); r++) {
                    for (int c = 0; c < grid.getCols(); c++) {
                        LetterTile t = grid.getTile(r, c);
                        if (t.getLetter() == letter) {
                            points = t.getValue();
                            break;
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
                
                tiles.getChildren().add(tile);
            }
            selectedWordLabel.setText("");
            selectedWordLabel.setGraphic(tiles);
            
            for (int i = 0; i < tiles.getChildren().size(); i++) {
                javafx.scene.Node n = tiles.getChildren().get(i);
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(Duration.millis(300), n);
                tt.setFromY(6);
                tt.setToY(0);
                tt.play();
            }
        }
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
            // Local: first Cast = player 1, second Cast = player 2 (grid stays enabled for
            // both)
            Wizard toSubmit = engine.getSkillCheckManager().hasSubmittedHalfHp(wizard1) ? wizard2 : wizard1;
            engine.submitHalfHpWord(toSubmit, word);
            feedbackLabel.setText(toSubmit.getName() + " submitted. " +
                    (engine.getSkillCheckManager().hasSubmittedHalfHp(wizard1)
                            && engine.getSkillCheckManager().hasSubmittedHalfHp(wizard2)
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
        SoundManager.startBgm("BattleMusic.wav"); // switch to battle music
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
                });
    }

    @Override
    public void onSpellCast(Spell spell, Wizard caster, Wizard target) {
        SoundManager.playCast(); // successful word launched
        String entry = getSpellEmoji(spell) + " " + spell.getSpellDescription();
        battleLog.getItems().add(0, entry);
        updateHpDisplay();
        renderGrid();

        feedbackLabel.setText(spell.getWord() + " dealt "
                + spell.getTotalDamage() + " damage!");
        feedbackLabel.setStyle("-fx-text-fill: #4caf50;");

        updateHpBarColor(
                caster == wizard1 ? player2HpBar : player1HpBar,
                target.getHpPercentage());

        selectedWordLabel.setText("");
        selectedWordLabel.setGraphic(null);
    }

    @Override
    public void onInvalidWord(String word) {
        SoundManager.playInvalid(); // key_delete sound for invalid submission
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
        selectedWordLabel.setText("");
        selectedWordLabel.setGraphic(null);
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
        SoundManager.playSkillCheck();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Skill Check Available!");
        alert.setHeaderText(initiator.getName() + " can initiate a challenge!");
        alert.setContentText(
                "You are at or below 50% HP.\n\n" +
                        "Initiate the Half HP Challenge?\n" +
                        "Win → opponent drops to your HP level\n" +
                        "Lose → you take word damage\n\n" +
                        "This can only be used once per match!");

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
        SoundManager.playSkillCheck();
        halfHpChallengeActive = true;
        currentSharedGrid = sharedGrid;

        skillCheckStatusLabel.setText(
                "⚔ HALF HP CHALLENGE — Both players spell a word!");
        skillCheckStatusLabel.setStyle("-fx-text-fill: #ff6b6b;");
        turnLabel.setText("⚔ SKILL CHECK");
        timerLabel.setStyle(
                "-fx-text-fill: #ff6b6b; -fx-font-size: 30px; -fx-font-weight: bold;");

        renderSharedGrid(sharedGrid);
        selectedWordLabel.setText("");
        selectedWordLabel.setGraphic(null);
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
        SoundManager.playSkillCheck();
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
        skillCheckSecondsRemaining = engine.getSkillCheckManager().getSkillCheckDuration();

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

    protected void stopSkillCheckTimer() {
        if (skillCheckTimerExecutor != null) {
            skillCheckTimerExecutor.shutdown();
            skillCheckTimerExecutor = null;
        }
    }

    // --- Helpers ---
    protected void updatePortraitHighlight(Wizard currentPlayer) {
        if (wizard1Portrait == null || wizard2Portrait == null)
            return;

        final String w1Bg = "-fx-background-color: #2a1a4a; ";
        final String w2Bg = "-fx-background-color: #1a2a4a; ";

        final String activeBorder = "-fx-border-color: #e2b96f; -fx-border-width: 4; " +
                "-fx-border-radius: 6; -fx-background-radius: 6;";
        final String inactiveBorder = "-fx-border-color: #444466; -fx-border-width: 1.5; " +
                "-fx-border-radius: 6; -fx-background-radius: 6;";

        if (currentPlayer == wizard1) {
            wizard1Portrait.setStyle(w1Bg + activeBorder);
            wizard2Portrait.setStyle(w2Bg + inactiveBorder);
        } else {
            wizard2Portrait.setStyle(w2Bg + activeBorder);
            wizard1Portrait.setStyle(w1Bg + inactiveBorder);
        }
    }

    protected void updateHpDisplay() {
        player1HpBar.setProgress(wizard1.getHpPercentage());
        player2HpBar.setProgress(wizard2.getHpPercentage());
        player1HpLabel.setText(wizard1.getHp() + " / " + wizard1.getMaxHp());
        player2HpLabel.setText(wizard2.getHp() + " / " + wizard2.getMaxHp());
        updateHpBarColor(player1HpBar, wizard1.getHpPercentage());
        updateHpBarColor(player2HpBar, wizard2.getHpPercentage());
    }

    protected void updateHpBarColor(ProgressBar bar, double percentage) {
        if (percentage > 0.5) {
            bar.setStyle("-fx-accent: #3fc53f;"); // Vibrant retro forest green
        } else if (percentage > 0.25) {
            bar.setStyle("-fx-accent: #ff9f3a;"); // Radiant pixel amber orange
        } else {
            bar.setStyle("-fx-accent: #ff4f4f;"); // Fiery pixel crimson red
        }
    }

    protected void setInputEnabled(boolean enabled) {
        castButton.setDisable(!enabled);
        letterGridPane.setDisable(!enabled);
    }

    protected String getIdleStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #424266 0%, #424266 3px, #2a2a4a 3px, #1a1a30 100%);" +
                "-fx-background-insets: 0;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #555588 #1a1a2a #1a1a2a #555588;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 0;" +
                "-fx-text-fill: #e2b96f;" +
                "-fx-font-family: 'Pixelify Sans';" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.55), 0, 0.0, 2, 2);";
    }

    protected String getHoverStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #50507d 0%, #50507d 3px, #33335c 3px, #202042 100%);" +
                "-fx-background-insets: 0;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #6e6eab #222238 #222238 #6e6eab;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 0;" +
                "-fx-text-fill: #ffffff;" +
                "-fx-font-family: 'Pixelify Sans';" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(110,110,180,0.18), 6, 0.1, 0, 0);";
    }

    protected String getSelectedStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #ffe9a0 0%, #ffe9a0 3px, #f0c040 3px, #e8a828 100%);" +
                "-fx-background-insets: 0;" +
                "-fx-background-radius: 0;" +
                "-fx-border-color: #fff5c0 #c09030 #c09030 #fff5c0;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 0;" +
                "-fx-text-fill: #1a1000;" +
                "-fx-font-family: 'Pixelify Sans';" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(255,200,60,0.22), 6, 0.1, 0, 0);";
    }

    protected String getSpellEmoji(Spell spell) {
        return switch (spell.getPower()) {
            case WEAK -> "🔹";
            case MODERATE -> "🔷";
            case STRONG -> "💥";
            case DEVASTATING -> "⚡";
        };
    }

    protected void showGameOverDialog(Wizard winner, Wizard loser) {
        SoundManager.stopBgm();
        SoundManager.playVictory(); // local 2P: play victory for the match winner

        javafx.stage.Stage dialog = com.rst.outspelled.util.DialogBuilder.buildDialogStage("Duel Over!", 400, 250);

        javafx.scene.control.Label headerLabel = com.rst.outspelled.util.DialogBuilder
                .styledDialogLabel(winner.getName() + " wins!");
        javafx.scene.control.Label contentLabel = new javafx.scene.control.Label(
                winner.getName() + " defeated " + loser.getName() + "!\n\nPlay again?");
        contentLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 14px;");

        javafx.scene.layout.StackPane playAgainBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("Rematch",
                true);
        playAgainBtn.setOnMouseClicked(e -> {
            SoundManager.playClick();
            dialog.close();
            engine.shutdown();
            GameController.setWizards(
                    new Wizard(wizard1.getName(), 200, wizard1.getSkin()),
                    new Wizard(wizard2.getName(), 200, wizard2.getSkin()));
            Main.navigateTo("game-view.fxml");
        });

        javafx.scene.layout.StackPane menuBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("Main Menu",
                false);
        menuBtn.setOnMouseClicked(e -> {
            SoundManager.playClick();
            dialog.close();
            engine.shutdown();
            Main.navigateTo("menu-view.fxml");
        });

        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(15, playAgainBtn, menuBtn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(20);
        body.setStyle("-fx-padding: 30 24 24 24;");
        body.setAlignment(javafx.geometry.Pos.CENTER);
        body.getChildren().addAll(headerLabel, contentLabel, btnBox);

        javafx.scene.layout.VBox dialogRoot = com.rst.outspelled.util.DialogBuilder.buildDialogRoot("🏆 Duel Over 🏆",
                false, body, dialog);
        dialog.getScene().setRoot(dialogRoot);
        dialog.showAndWait();
    }

    @FXML
    protected void onOptionsClicked() {
        SoundManager.playClick();
        showSettingsDialog();
    }

    private void showSettingsDialog() {
        javafx.stage.Stage dialog = com.rst.outspelled.util.DialogBuilder.buildDialogStage("Settings", 400, 380);

        javafx.scene.control.Label headerLabel = com.rst.outspelled.util.DialogBuilder
                .styledDialogLabel("Adjust Game Volumes");
        headerLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-text-fill: #8899aa;");

        // BGM Slider
        javafx.scene.control.Label bgmLabel = new javafx.scene.control.Label("Background Music");
        bgmLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 14px; -fx-text-fill: #e2b96f;");
        javafx.scene.control.Slider bgmSlider = new javafx.scene.control.Slider(0, 1.0, SoundManager.getBgmVolume());
        bgmSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            SoundManager.setBgmVolume(newVal.doubleValue());
        });

        // SFX Slider
        javafx.scene.control.Label sfxLabel = new javafx.scene.control.Label("Sound Effects");
        sfxLabel.setStyle("-fx-font-family: 'Pixelify Sans'; -fx-font-size: 14px; -fx-text-fill: #e2b96f;");
        javafx.scene.control.Slider sfxSlider = new javafx.scene.control.Slider(0, 1.0, SoundManager.getSfxVolume());
        sfxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            SoundManager.setSfxVolume(newVal.doubleValue());
        });
        sfxSlider.setOnMouseReleased(e -> SoundManager.playClick());

        javafx.scene.layout.VBox bgmBox = new javafx.scene.layout.VBox(5, bgmLabel, bgmSlider);
        javafx.scene.layout.VBox sfxBox = new javafx.scene.layout.VBox(5, sfxLabel, sfxSlider);
        bgmBox.setAlignment(javafx.geometry.Pos.CENTER);
        sfxBox.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.StackPane closeBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("Resume",
                true);
        closeBtn.setOnMouseClicked(e -> {
            SoundManager.playClick();
            dialog.close();
        });

        javafx.scene.layout.StackPane quitBtn = com.rst.outspelled.util.DialogBuilder.buildDialogButton("Quit to Menu",
                false);
        quitBtn.setOnMouseClicked(e -> {
            SoundManager.playClick();
            dialog.close();
            engine.shutdown();
            Main.navigateTo("menu-view.fxml");
        });

        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(10, closeBtn, quitBtn);
        buttons.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(24);
        body.setStyle("-fx-padding: 30 24 24 24;");
        body.setAlignment(javafx.geometry.Pos.CENTER);
        body.getChildren().addAll(headerLabel, bgmBox, sfxBox, buttons);

        javafx.scene.layout.VBox dialogRoot = com.rst.outspelled.util.DialogBuilder.buildDialogRoot("⚙  Options  ⚙",
                false, body, dialog);
        dialog.getScene().setRoot(dialogRoot);
        dialog.showAndWait();
    }

    protected void handleKeyInput(javafx.scene.input.KeyEvent event) {
        String key = event.getText();
        javafx.scene.input.KeyCode code = event.getCode();

        LetterGrid grid = halfHpChallengeActive && currentSharedGrid != null
                ? currentSharedGrid
                : engine.getCurrentGrid();

        if (code == javafx.scene.input.KeyCode.BACK_SPACE) {
            String currentWord = grid.getSelectedWord();
            if (!currentWord.isEmpty()) {
                SoundManager.playKeyDelete(); // delete key sound
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
                SoundManager.playKeyTap(); // key tap for each letter selected
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