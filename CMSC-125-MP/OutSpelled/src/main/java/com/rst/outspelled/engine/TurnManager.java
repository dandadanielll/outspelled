package com.rst.outspelled.engine;

import com.rst.outspelled.model.Wizard;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TurnManager {

    public interface TurnListener {
        void onTurnChanged(Wizard currentPlayer);
        void onTimerTick(int secondsRemaining);
        void onTimeExpired(Wizard currentPlayer);
    }

    private static final int TURN_DURATION_SECONDS = 30;

    private final Wizard player1;
    private final Wizard player2;
    private Wizard currentPlayer;
    private TurnListener listener;
    private int secondsRemaining;
    private volatile boolean timerRunning;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("TurnTimerThread");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> timerTask;

    public TurnManager(Wizard player1, Wizard player2, TurnListener listener) {
        this.player1 = player1;
        this.player2 = player2;
        this.listener = listener;
        this.currentPlayer = player1;
        player1.setMyTurn(true);
        player2.setMyTurn(false);
    }

    public void startTurn() {
        stopTimer(); // cancel any previous task so we never have two timers
        secondsRemaining = TURN_DURATION_SECONDS;
        timerRunning = true;

        timerTask = scheduler.scheduleAtFixedRate(() -> {
            if (!timerRunning) return;

            Platform.runLater(() ->
                    listener.onTimerTick(secondsRemaining)
            );

            if (secondsRemaining <= 0) {
                timerRunning = false;
                timerTask.cancel(false);
                Platform.runLater(() ->
                        listener.onTimeExpired(currentPlayer)
                );
            }

            secondsRemaining--;

        }, 0, 1, TimeUnit.SECONDS);
    }

    public void endTurn() {
        stopTimer();
        switchPlayer();
        Platform.runLater(() ->
                listener.onTurnChanged(currentPlayer)
        );
        startTurn();
    }

    public void stopTimer() {
        timerRunning = false;
        if (timerTask != null) {
            timerTask.cancel(false);
            timerTask = null;
        }
    }

    private void switchPlayer() {
        if (currentPlayer == player1) {
            currentPlayer = player2;
            player1.setMyTurn(false);
            player2.setMyTurn(true);
        } else {
            currentPlayer = player1;
            player2.setMyTurn(false);
            player1.setMyTurn(true);
        }
    }

    public void shutdown() {
        stopTimer();
        scheduler.shutdown();
    }

    // Getters
    public Wizard getCurrentPlayer() { return currentPlayer; }
    public int getSecondsRemaining() { return secondsRemaining; }
    public boolean isTimerRunning() { return timerRunning; }
    public int getTurnDuration() { return TURN_DURATION_SECONDS; }
}