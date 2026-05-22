package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.ai.AiOpponent;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.Wizard;
import javafx.scene.control.*;

// Extends from GameController and only overrides the parts that differ for singleplayer. All shared UI logic is inherited. 
public class SoloGameController extends GameController {

    private static AiOpponent aiOpponent;

    // Called by MenuController before navigating to the sologame scene
    public static void setup(Wizard human, Wizard ai, AiOpponent opponent) {
        GameController.setWizards(human, ai);
        aiOpponent = opponent;
    }

    @Override
    public void initialize() {
        super.initialize(); // sets up engine, HP display, grid, etc.
        player2NameLabel.setText(wizard2.getName() + "(AI)"); // mark as AI in the UI
    }

    @Override
    public void onTurnChanged(Wizard currentPlayer) {
        super.onTurnChanged(currentPlayer); // handles all UI updates (turn label, grid, portraits)
        if (currentPlayer == wizard2) {
            // Lock human input and trigger AI turn
            setInputEnabled(false);
            feedbackLabel.setText("Opponent is thinking...");
            feedbackLabel.setStyle("-fx-text-fill: #a0a0c0;");
            aiOpponent.takeTurn(engine.getPlayer2Grid(), (word, grid) -> {
                if (word == null || word.isBlank()) {
                    battleLog.getItems().add(0, "Opponent couldn't form a word!");
                } else {
                    feedbackLabel.setText("Opponent casts: " + word.toUpperCase() + "!");
                    feedbackLabel.setStyle("-fx-text-fill: #7eb8e2;");
                    engine.submitWord(word, grid);
                }
            });
        }
    }

    @Override
    public void onInvalidWord(String word) {
        super.onInvalidWord(word); // shows the feedback message and reenables input
        // Base class reenables input after an invalid word, but not if it's the AI's
        // turn
        if (engine.getCurrentPlayer() == wizard2)
            setInputEnabled(false);
    }

    @Override
    public void onHalfHpPrompt(Wizard initiator) {
        if (initiator == wizard2) {
            // AI decides automatically based on its difficulty
            if (aiOpponent.shouldInitiateHalfHp())
                engine.initiateHalfHpChallenge(wizard2);
            else
                engine.skipHalfHpChallenge();
        } else {
            super.onHalfHpPrompt(initiator); // show confirmation dialog for human player
        }
    }

    @Override
    public void onHalfHpChallengeStart(LetterGrid sharedGrid) {
        super.onHalfHpChallengeStart(sharedGrid); // sets up shared grid UI + skill check timer
        // AI also picks a word from the shared grid after its thinking delay
        aiOpponent.takeTurn(sharedGrid, (word, grid) -> {
            if (word != null && !word.isBlank())
                engine.submitHalfHpWord(wizard2, word);
        });
    }

    @Override
    public void onLastStandStart(String scrambledWord) {
        super.onLastStandStart(scrambledWord); // shows overlay, starts human input + timer
        // AI attempts to unscramble (difficulty determines if it actually succeeds in
        // time)
        aiOpponent.attemptLastStand(
                engine.getSkillCheckManager().getUnscrambledTarget(), engine, wizard2);
    }

    @Override
    protected void showGameOverDialog(Wizard winner, Wizard loser) {
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
            aiOpponent.shutdown();
            if (response == playAgain) {
                SoloGameController.setup(
                        new Wizard(wizard1.getName(), 200, wizard1.getSkin()),
                        new Wizard(wizard2.getName(), 200, wizard2.getSkin()),
                        new AiOpponent(aiOpponent.getBrain()));
                Main.navigateTo("solo-game-view.fxml");
            } else {
                Main.navigateTo("menu-view.fxml");
            }
        });
    }
}
