package com.rst.outspelled.engine;

import com.rst.outspelled.model.Wizard;

public class SkillCheckResult {

    public enum SkillCheckType {
        HALF_HP_CHALLENGE,
        LAST_STAND
    }

    public enum Outcome {
        INITIATOR_WINS,
        INITIATOR_LOSES,
        TIE
    }

    private final SkillCheckType type;
    private final Wizard initiator;
    private final Wizard opponent;
    private final Outcome outcome;
    private final long initiatorTimeMs;
    private final long opponentTimeMs;
    private final String initiatorWord;
    private final String opponentWord;
    private final int damageOrHeal;

    public SkillCheckResult(SkillCheckType type, Wizard initiator,
                            Wizard opponent, Outcome outcome,
                            long initiatorTimeMs, long opponentTimeMs,
                            String initiatorWord, String opponentWord,
                            int damageOrHeal) {
        this.type = type;
        this.initiator = initiator;
        this.opponent = opponent;
        this.outcome = outcome;
        this.initiatorTimeMs = initiatorTimeMs;
        this.opponentTimeMs = opponentTimeMs;
        this.initiatorWord = initiatorWord;
        this.opponentWord = opponentWord;
        this.damageOrHeal = damageOrHeal;
    }

    public String getSummary() {
        return switch (type) {
            case HALF_HP_CHALLENGE -> switch (outcome) {
                case INITIATOR_WINS ->
                        initiator.getName() + " won the challenge! "
                                + opponent.getName() + " drops to "
                                + opponent.getHp() + " HP!";
                case INITIATOR_LOSES ->
                        initiator.getName() + " lost the challenge and took "
                                + damageOrHeal + " damage!";
                case TIE ->
                        "The challenge ended in a tie! No damage dealt.";
            };
            case LAST_STAND -> switch (outcome) {
                case INITIATOR_WINS ->
                        initiator.getName() + " survived the Last Stand! +"
                                + damageOrHeal + " HP!";
                case INITIATOR_LOSES ->
                        opponent.getName() + " blocked the Last Stand! "
                                + initiator.getName() + " gains nothing.";
                case TIE ->
                        "Nobody answered in time! No HP gained.";
            };
        };
    }

    // Getters
    public SkillCheckType getType() { return type; }
    public Wizard getInitiator() { return initiator; }
    public Wizard getOpponent() { return opponent; }
    public Outcome getOutcome() { return outcome; }
    public long getInitiatorTimeMs() { return initiatorTimeMs; }
    public long getOpponentTimeMs() { return opponentTimeMs; }
    public String getInitiatorWord() { return initiatorWord; }
    public String getOpponentWord() { return opponentWord; }
    public int getDamageOrHeal() { return damageOrHeal; }
}