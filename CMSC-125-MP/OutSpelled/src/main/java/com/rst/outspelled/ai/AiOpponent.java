package com.rst.outspelled.ai;

import com.rst.outspelled.engine.GameEngine;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.Wizard;
import javafx.application.Platform;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


// Schedules AI turns on a background thread, then fires callbacks on the FX
// thread
public class AiOpponent {

    // Callback fired on the JavaFX thread after the AI has chosen a word
    public interface TurnCallback {
        void onWordChosen(String word, LetterGrid grid);
    }

    private final AiBrain brain;

    // Tracks every word the AI has already submitted to prevent repetition
    private final Set<String> usedWords = Collections.synchronizedSet(new HashSet<>());

    // Background thread pool for AI turn processing
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r); // thread that runs task r
        t.setName("AiOpponentThread");
        t.setDaemon(true); // allows program exit even if threads still running
        return t;
    });

    // constructor
    public AiOpponent(AiBrain brain) {
        this.brain = brain;
    }

    /*
     * Called by SoloGameController when it is the AI's turn
     * Waits for the brain's thinking delay, finds a word, then fires
     * the callback on the JavaFX application thread
     */
    public void takeTurn(LetterGrid grid, TurnCallback callback) {
        executor.schedule(() -> {
            String word = brain.findBestWord(grid, usedWords); // passes the current grid to the ai, excluding used words
            if (word != null && !word.isBlank()) {
                usedWords.add(word.toLowerCase()); // remember this word so it won't be repeated

                // Select the matching tiles on the actual grid so that grid.confirmWord()
                // (called inside GameEngine) correctly marks them as used and replaces them.
                grid.deselectAll(); // clear any stale selection first
                for (char c : word.toCharArray()) {
                    grid.selectFirstMatchingTile(c);
                }
            }
            Platform.runLater(() -> callback.onWordChosen(word, grid)); // schedules the callback to run on the fx thread
        }, brain.getThinkingDelayMs(), TimeUnit.MILLISECONDS);
    }

    // decides whether AI initiates the half hp skillcheck
    public boolean shouldInitiateHalfHp() {
        return Math.random() < 0.5; // default: 50% chance
    }

    /*
     * AI process for the Last Stand skill check
     * sometimes AI times out intentionally
     * 
     * @param unscrambledTarget the correct answer
     * 
     * @param engine the running GameEngine
     * 
     * @param aiWizard the AI's Wizard instance
     */
    public void attemptLastStand(String unscrambledTarget, GameEngine engine, Wizard aiWizard) {
        // Medium: wait longer than the skill check timer so AI effectively never
        // answers
        long delayMs = 60_000L;
        executor.schedule(() -> Platform.runLater(() -> engine.submitLastStandWord(aiWizard, unscrambledTarget)),
                delayMs, TimeUnit.MILLISECONDS);
    }

    /** Shuts down the background executor, called when the game ends */
    public void shutdown() {
        executor.shutdownNow();
        usedWords.clear();
    }

    // Exposes the brain so SoloGameController can type check difficulty if needed
    public AiBrain getBrain() {
        return brain;
    }
}
