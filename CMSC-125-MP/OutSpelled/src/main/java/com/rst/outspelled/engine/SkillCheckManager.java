package com.rst.outspelled.engine;

import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.util.LetterValues;

import java.util.*;

public class SkillCheckManager {

    public interface SkillCheckListener {
        void onHalfHpPrompt(Wizard initiator);
        void onHalfHpChallengeStart(LetterGrid sharedGrid);
        void onHalfHpChallengeComplete(SkillCheckResult result);
        void onLastStandStart(String scrambledWord);
        void onLastStandComplete(SkillCheckResult result);
    }

    private static final int SKILL_CHECK_DURATION_SECONDS = 15;
    private static final int SPEED_BONUS_MAX = 30;
    private static final int LAST_STAND_MAX_HP_GAIN = 50;

    private static final String[] WORD_POOL = {
            "WIZARD", "ARCANE", "MYSTIC", "CIPHER", "BLAZE",
            "QUARTZ", "VORTEX", "SPECTER", "DRAGON", "RUNE",
            "SPELL", "CURSE", "EMBER", "STORM", "FROST"
    };

    private final Wizard player1;
    private final Wizard player2;
    private final SkillCheckListener listener;
    private final Random random = new Random();

    // state
    private boolean halfHpCheckUsed = false;
    private boolean lastStandActive = false;
    private boolean halfHpActive = false;
    private Wizard halfHpInitiator = null;
    /** Each player can have at most one last stand per game. */
    private final Set<Wizard> lastStandUsed = new HashSet<>();

    // shared tracking
    private long skillCheckStartTime;
    private LetterGrid sharedGrid;
    private String targetWord;
    private String unscrambledTarget;

    // per player submission tracking
    private final Map<Wizard, String> submittedWords = new HashMap<>();
    private final Map<Wizard, Long> submitTimes = new HashMap<>();

    public SkillCheckManager(Wizard player1, Wizard player2,
                             SkillCheckListener listener) {
        this.player1 = player1;
        this.player2 = player2;
        this.listener = listener;
    }

    // -------------------------------------------------------
    // HALF HP CHALLENGE
    // -------------------------------------------------------

    public void checkHalfHpTrigger(Wizard damagedPlayer, boolean isEndOfRound) {
        if (halfHpCheckUsed) return;
        if (lastStandActive) return;
        if (!isEndOfRound) return;
        if (damagedPlayer.getHpPercentage() <= 0.5) {
            listener.onHalfHpPrompt(damagedPlayer);
        }
    }

    public void initiateHalfHpChallenge(Wizard initiator) {
        if (halfHpCheckUsed) return;
        halfHpCheckUsed = true;
        halfHpActive = true;
        halfHpInitiator = initiator;

        submittedWords.clear();
        submitTimes.clear();
        sharedGrid = new LetterGrid();
        skillCheckStartTime = System.currentTimeMillis();

        listener.onHalfHpChallengeStart(sharedGrid);
    }

    public void skipHalfHpChallenge() {
        halfHpCheckUsed = true;
        halfHpActive = false;
        halfHpInitiator = null;
    }

    public void submitHalfHpWord(Wizard player, String word) {
        if (!halfHpActive) return;
        if (submittedWords.containsKey(player)) return;
        if (word == null || word.isBlank()) return;

        submittedWords.put(player, word);
        submitTimes.put(player, System.currentTimeMillis() - skillCheckStartTime);

        // resolve once both players submitted
        if (submittedWords.size() == 2) {
            resolveHalfHpChallenge();
        }
    }

    public void forceResolveHalfHp() {
        // called when skill check timer expires
        if (!halfHpActive) return;
        if (submittedWords.size() < 2) {
            resolveHalfHpChallenge();
        }
    }

    private void resolveHalfHpChallenge() {
        halfHpActive = false;

        String w1Word = submittedWords.getOrDefault(player1, "");
        String w2Word = submittedWords.getOrDefault(player2, "");
        long w1Time = submitTimes.getOrDefault(player1,
                (long)(SKILL_CHECK_DURATION_SECONDS * 1000));
        long w2Time = submitTimes.getOrDefault(player2,
                (long)(SKILL_CHECK_DURATION_SECONDS * 1000));

        int p1Score = w1Word.isEmpty() ? 0 : scoreWithSpeed(w1Word, w1Time);
        int p2Score = w2Word.isEmpty() ? 0 : scoreWithSpeed(w2Word, w2Time);

        Wizard opponent = halfHpInitiator == player1 ? player2 : player1;
        int initiatorScore = halfHpInitiator == player1 ? p1Score : p2Score;
        int opponentScore = halfHpInitiator == player1 ? p2Score : p1Score;
        String initiatorWord = halfHpInitiator == player1 ? w1Word : w2Word;
        long initiatorTime = halfHpInitiator == player1 ? w1Time : w2Time;

        SkillCheckResult.Outcome outcome;
        int damageOrHeal;

        if (initiatorScore >= opponentScore) {
            outcome = SkillCheckResult.Outcome.INITIATOR_WINS;
            int hpDiff = opponent.getHp() - halfHpInitiator.getHp();
            damageOrHeal = Math.max(0, hpDiff);
            opponent.takeDamage(damageOrHeal);
        } else {
            outcome = SkillCheckResult.Outcome.INITIATOR_LOSES;
            damageOrHeal = initiatorWord.isEmpty() ? 0
                    : LetterValues.getWordValue(initiatorWord)
                    + initiatorWord.length();
            halfHpInitiator.takeDamage(damageOrHeal);
        }

        SkillCheckResult result = new SkillCheckResult(
                SkillCheckResult.SkillCheckType.HALF_HP_CHALLENGE,
                halfHpInitiator, opponent, outcome,
                initiatorTime,
                halfHpInitiator == player1 ? w2Time : w1Time,
                initiatorWord,
                halfHpInitiator == player1 ? w2Word : w1Word,
                damageOrHeal
        );

        listener.onHalfHpChallengeComplete(result);
    }

    // -------------------------------------------------------
    // LAST STAND
    // -------------------------------------------------------

    public void checkLastStandTrigger(Wizard damagedPlayer) {
        if (lastStandActive) return;
        if (halfHpActive) return;
        if (lastStandUsed.contains(damagedPlayer)) return; // once per player per game
        if (damagedPlayer.getHpPercentage() <= 0.1
                && !damagedPlayer.isDefeated()) {
            lastStandUsed.add(damagedPlayer);
            lastStandActive = true;
            startLastStand(damagedPlayer);
        }
    }

    private void startLastStand(Wizard initiator) {
        submittedWords.clear();
        submitTimes.clear();

        // pick a word and scramble it
        unscrambledTarget = WORD_POOL[random.nextInt(WORD_POOL.length)];
        targetWord = scramble(unscrambledTarget);

        // make sure scrambled != original
        while (targetWord.equals(unscrambledTarget)) {
            targetWord = scramble(unscrambledTarget);
        }

        skillCheckStartTime = System.currentTimeMillis();
        listener.onLastStandStart(targetWord);
    }

    public void submitLastStandWord(Wizard player, String word) {
        if (!lastStandActive) return;
        if (submittedWords.containsKey(player)) return;
        if (word == null || word.isBlank()) return;

        // only accept correct answer
        if (!word.trim().equalsIgnoreCase(unscrambledTarget)) return;

        submittedWords.put(player, word);
        submitTimes.put(player,
                System.currentTimeMillis() - skillCheckStartTime);

        // first correct answer resolves immediately
        resolveLastStand();
    }

    public void forceResolveLastStand() {
        if (!lastStandActive) return;
        resolveLastStand();
    }

    private void resolveLastStand() {
        lastStandActive = false;

        if (submittedWords.isEmpty()) {
            // nobody answered in time — no HP gain, no damage
            SkillCheckResult result = new SkillCheckResult(
                    SkillCheckResult.SkillCheckType.LAST_STAND,
                    player1, player2,
                    SkillCheckResult.Outcome.TIE,
                    -1, -1, "", "", 0
            );
            listener.onLastStandComplete(result);
            return;
        }

        // find fastest correct answer
        Wizard winner = null;
        long fastestTime = Long.MAX_VALUE;
        for (Map.Entry<Wizard, Long> entry : submitTimes.entrySet()) {
            if (entry.getValue() < fastestTime) {
                fastestTime = entry.getValue();
                winner = entry.getKey();
            }
        }

        int hpGain = Math.max(1, calculateLastStandHpGain(fastestTime)); // ensure at least 1 HP
        int newHp = Math.min(winner.getHp() + hpGain, winner.getMaxHp());
        winner.setHp(newHp);

        SkillCheckResult.Outcome outcome = winner == player1
                ? SkillCheckResult.Outcome.INITIATOR_WINS
                : SkillCheckResult.Outcome.INITIATOR_LOSES;

        SkillCheckResult result = new SkillCheckResult(
                SkillCheckResult.SkillCheckType.LAST_STAND,
                player1, player2, outcome,
                submitTimes.getOrDefault(player1, -1L),
                submitTimes.getOrDefault(player2, -1L),
                submittedWords.getOrDefault(player1, ""),
                submittedWords.getOrDefault(player2, ""),
                hpGain
        );

        listener.onLastStandComplete(result);
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------

    private int scoreWithSpeed(String word, long timeMs) {
        int wordScore = LetterValues.getWordValue(word) + word.length();
        int speedBonus = (int) Math.max(0,
                SPEED_BONUS_MAX - (timeMs / 1000.0));
        return wordScore + speedBonus;
    }

    private int calculateLastStandHpGain(long timeMs) {
        double secondsTaken = timeMs / 1000.0;
        double ratio = Math.max(0,
                1.0 - (secondsTaken / SKILL_CHECK_DURATION_SECONDS));
        return (int) Math.round(LAST_STAND_MAX_HP_GAIN * ratio);
    }

    private String scramble(String word) {
        List<Character> chars = new ArrayList<>();
        for (char c : word.toCharArray()) chars.add(c);
        Collections.shuffle(chars);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);
        return sb.toString();
    }

    /** For local play: which player has already submitted in the half-HP challenge. */
    public boolean hasSubmittedHalfHp(Wizard w) { return submittedWords.containsKey(w); }

    // Getters
    public boolean isHalfHpCheckUsed() { return halfHpCheckUsed; }
    public boolean isLastStandActive() { return lastStandActive; }
    public boolean isHalfHpActive() { return halfHpActive; }
    public LetterGrid getSharedGrid() { return sharedGrid; }
    public String getTargetWord() { return targetWord; }
    public String getUnscrambledTarget() { return unscrambledTarget; }
    public int getSkillCheckDuration() { return SKILL_CHECK_DURATION_SECONDS; }
    public Wizard getHalfHpInitiator() { return halfHpInitiator; }
}