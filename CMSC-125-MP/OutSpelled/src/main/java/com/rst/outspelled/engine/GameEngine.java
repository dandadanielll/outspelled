package com.rst.outspelled.engine;

import com.rst.outspelled.damage.DamageCalculator;
import com.rst.outspelled.dictionary.DictionaryLoader;
import com.rst.outspelled.dictionary.WordValidator;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.Spell;
import com.rst.outspelled.model.Wizard;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameEngine {

    public enum GameState {
        WAITING,
        LOADING,
        IN_PROGRESS,
        GAME_OVER
    }

    public interface GameListener {
        void onGameReady();
        void onSpellCast(Spell spell, Wizard caster, Wizard target);
        void onInvalidWord(String word);
        void onPlayerDefeated(Wizard loser, Wizard winner);
        void onTurnChanged(Wizard currentPlayer);
        void onTimerTick(int secondsRemaining);
        void onTimeExpired(Wizard currentPlayer);

        // skill checks
        void onHalfHpPrompt(Wizard initiator);
        void onHalfHpChallengeStart(LetterGrid sharedGrid);
        void onHalfHpChallengeComplete(SkillCheckResult result);
        void onLastStandStart(String scrambledWord);
        void onLastStandComplete(SkillCheckResult result);
    }

    private final Wizard player1;
    private final Wizard player2;
    private final TurnManager turnManager;
    private final GameListener listener;
    private final List<Spell> spellHistory;
    private volatile GameState state;

    private final LetterGrid player1Grid = new LetterGrid();
    private final LetterGrid player2Grid = new LetterGrid();

    private final SkillCheckManager skillCheckManager;
    private boolean roundPlayer1Went = false;
    private boolean roundPlayer2Went = false;

    public GameEngine(Wizard player1, Wizard player2, GameListener listener) {
        this.player1 = player1;
        this.player2 = player2;
        this.listener = listener;
        this.spellHistory = Collections.synchronizedList(new ArrayList<>());
        this.state = GameState.WAITING;

        this.turnManager = new TurnManager(player1, player2,
                new TurnManager.TurnListener() {
                    @Override
                    public void onTurnChanged(Wizard currentPlayer) {
                        listener.onTurnChanged(currentPlayer);
                    }

                    @Override
                    public void onTimerTick(int secondsRemaining) {
                        listener.onTimerTick(secondsRemaining);
                    }

                    @Override
                    public void onTimeExpired(Wizard currentPlayer) {
                        listener.onTimeExpired(currentPlayer);
                        turnManager.endTurn();
                    }
                });

        this.skillCheckManager = new SkillCheckManager(player1, player2,
                new SkillCheckManager.SkillCheckListener() {

                    @Override
                    public void onHalfHpPrompt(Wizard initiator) {
                        turnManager.stopTimer();
                        Platform.runLater(() ->
                                listener.onHalfHpPrompt(initiator));
                    }

                    @Override
                    public void onHalfHpChallengeStart(LetterGrid sharedGrid) {
                        Platform.runLater(() ->
                                listener.onHalfHpChallengeStart(sharedGrid));
                    }

                    @Override
                    public void onHalfHpChallengeComplete(SkillCheckResult result) {
                        Platform.runLater(() -> {
                            listener.onHalfHpChallengeComplete(result);
                            if (state == GameState.IN_PROGRESS) {
                                turnManager.startTurn();
                            }
                        });
                    }

                    @Override
                    public void onLastStandStart(String scrambledWord) {
                        turnManager.stopTimer();
                        Platform.runLater(() ->
                                listener.onLastStandStart(scrambledWord));
                    }

                    @Override
                    public void onLastStandComplete(SkillCheckResult result) {
                        Platform.runLater(() -> {
                            listener.onLastStandComplete(result);
                            if (state == GameState.IN_PROGRESS) {
                                turnManager.startTurn();
                            }
                        });
                    }
                });
    }

    public void initialize() {
        state = GameState.LOADING;

        DictionaryLoader.loadAsync(() -> {
            state = GameState.IN_PROGRESS;
            Platform.runLater(() -> {
                listener.onGameReady();
                turnManager.startTurn();
            });
        });
    }

    public void submitWord(String word, LetterGrid grid) {
        if (state != GameState.IN_PROGRESS) return;
        if (word == null || word.isBlank()) return;

        Wizard caster = turnManager.getCurrentPlayer();
        Wizard target = getOpponent(caster);

        WordValidator.validateAsync(word, isValid -> {
            if (isValid) {
                grid.confirmWord();

                Spell spell = new Spell(word, caster.getName());
                spellHistory.add(spell);
                DamageCalculator.applyDamage(spell, target);

                // track round completion
                if (caster == player1) roundPlayer1Went = true;
                if (caster == player2) roundPlayer2Went = true;
                boolean isEndOfRound = roundPlayer1Went && roundPlayer2Went;
                if (isEndOfRound) {
                    roundPlayer1Went = false;
                    roundPlayer2Went = false;
                }

                Platform.runLater(() -> {
                    listener.onSpellCast(spell, caster, target);

                    if (target.isDefeated()) {
                        state = GameState.GAME_OVER;
                        turnManager.shutdown();
                        listener.onPlayerDefeated(target, caster);
                        caster.recordWin();
                        target.recordLoss();
                    } else {
                        // check last stand first
                        skillCheckManager.checkLastStandTrigger(target);

                        // half HP: at end of round, check both players (whoever is at 50% can initiate)
                        if (!skillCheckManager.isLastStandActive()) {
                            if (isEndOfRound) {
                                skillCheckManager.checkHalfHpTrigger(target, true);
                                if (!skillCheckManager.isHalfHpActive()) {
                                    skillCheckManager.checkHalfHpTrigger(getOpponent(target), true);
                                }
                            }
                        }

                        // only end turn if no skill check triggered
                        if (!skillCheckManager.isLastStandActive()
                                && !skillCheckManager.isHalfHpActive()) {
                            turnManager.endTurn();
                        }
                    }
                });

            } else {
                Platform.runLater(() -> listener.onInvalidWord(word));
            }
        });
    }

    // --- Skill Check Public Methods ---

    public void initiateHalfHpChallenge(Wizard initiator) {
        skillCheckManager.initiateHalfHpChallenge(initiator);
    }

    public void skipHalfHpChallenge() {
        skillCheckManager.skipHalfHpChallenge();
        turnManager.endTurn();
    }

    public void submitHalfHpWord(Wizard player, String word) {
        skillCheckManager.submitHalfHpWord(player, word);
    }

    public void submitLastStandWord(Wizard player, String word) {
        skillCheckManager.submitLastStandWord(player, word);
    }

    public void forceResolveHalfHp() {
        skillCheckManager.forceResolveHalfHp();
    }

    public void forceResolveLastStand() {
        skillCheckManager.forceResolveLastStand();
    }

    private Wizard getOpponent(Wizard current) {
        return current == player1 ? player2 : player1;
    }

    public void shutdown() {
        turnManager.shutdown();
        WordValidator.shutdown();
        state = GameState.GAME_OVER;
    }

    // Getters
    public GameState getState() { return state; }
    public Wizard getPlayer1() { return player1; }
    public Wizard getPlayer2() { return player2; }
    public Wizard getCurrentPlayer() { return turnManager.getCurrentPlayer(); }
    public List<Spell> getSpellHistory() { return spellHistory; }
    public int getSecondsRemaining() { return turnManager.getSecondsRemaining(); }
    public LetterGrid getPlayer1Grid() { return player1Grid; }
    public LetterGrid getPlayer2Grid() { return player2Grid; }
    public SkillCheckManager getSkillCheckManager() { return skillCheckManager; }

    public LetterGrid getCurrentGrid() {
        return turnManager.getCurrentPlayer() == player1
                ? player1Grid : player2Grid;
    }
}