package com.rst.outspelled.ai;

import com.rst.outspelled.engine.GameEngine;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.Wizard;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


interface AiBrain {
    /*Find the best word the AI can form from the given grid
    Returns an empty string if no valid word is found*/
    String findBestWord(LetterGrid grid);

    // Simulated thinking delay in milliseconds before the AI submits (varies with difficulty)
    long getThinkingDelayMs();
}

//Schedules AI turns on a background thread, then fires callbacks on the FX thread
public class AiOpponent {

    //Callback fired on the JavaFX thread after the AI has chosen a word
    public interface TurnCallback {
        void onWordChosen(String word, LetterGrid grid);
    }

    private final AiBrain brain;

    //Background thread pool for AI turn processing
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r); //thread that runs task r
                t.setName("AiOpponentThread");
                t.setDaemon(true); //allows program exit even if threads still running
                return t;
            });

    //constructor
    public AiOpponent(AiBrain brain) {
        this.brain = brain;
    }

    /* Called by SoloGameController when it is the AI's turn
      Waits for the brain's thinking delay, finds a word, then fires
      the callback on the JavaFX application thread
     */
    public void takeTurn(LetterGrid grid, TurnCallback callback) {
        executor.schedule(() -> {
            String word = brain.findBestWord(grid); //passes the current grid to the ai (runs on background thread)

            // Select the matching tiles on the actual grid so that grid.confirmWord()(called inside GameEngine) correctly marks them as used and replaces them.
            if (word != null && !word.isBlank()) {
                grid.deselectAll(); // clear any previously selected tiles from its grid
                for (char c : word.toCharArray()) {
                    grid.selectFirstMatchingTile(c);
                }
            }

            Platform.runLater(() -> callback.onWordChosen(word, grid)); //schedules the callback (next step to do) to run on the fx thread
        }, brain.getThinkingDelayMs(), TimeUnit.MILLISECONDS);
    }

    /* decides whether AI initiates the half hp skillcheck
     (will branch per difficulty once Easy and Hard are added) */
    public boolean shouldInitiateHalfHp() {
        return Math.random() < 0.5; // medium default: 50% chance
    }

    /* AI process for the Last Stand skill check
     *Medium AI times out intentionally (will vary per difficulty later)
     *@param unscrambledTarget the correct answer
     *@param engine            the running GameEngine
     *@param aiWizard          the AI's Wizard instance 
     */
    public void attemptLastStand(String unscrambledTarget, GameEngine engine, Wizard aiWizard) {
        // 50% chance to answer quickly (about 3s), 50% chance to time out intentionally
        long delayMs = Math.random() < 0.5 ? 14_000L : 16_000L; //answers in 14s if success, 16 if not. Still gives a chance for the player to catchup this way
        executor.schedule(() ->
                Platform.runLater(() ->
                        engine.submitLastStandWord(aiWizard, unscrambledTarget)
                ), delayMs, TimeUnit.MILLISECONDS);
    }

    /** Shuts down the background executor, called when the game ends */
    public void shutdown() {
        executor.shutdownNow();
    }

    /** Exposes the brain so SoloGameController can type check difficulty if needed */
    public AiBrain getBrain() {
        return brain;
    }
}
