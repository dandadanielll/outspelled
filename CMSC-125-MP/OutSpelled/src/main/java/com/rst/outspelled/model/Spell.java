package com.rst.outspelled.model;

import com.rst.outspelled.util.LetterValues;

public class Spell {

    public enum SpellPower {
        WEAK,       // damage 1-10
        MODERATE,   // damage 11-20
        STRONG,     // damage 21-35
        DEVASTATING // damage 36+
    }

    private final String word;
    private final int letterValue;
    private final int wordLengthBonus;
    private final int totalDamage;
    private final SpellPower power;
    private final String casterName;
    private final long timestamp;

    public Spell(String word, String casterName) {
        this.word = word.toUpperCase();
        this.casterName = casterName;
        this.timestamp = System.currentTimeMillis();
        this.letterValue = LetterValues.getWordValue(word);
        this.wordLengthBonus = calculateLengthBonus(word);
        this.totalDamage = letterValue + wordLengthBonus;
        this.power = classifyPower(totalDamage);
    }

    private int calculateLengthBonus(String word) {
        int length = word.length();
        if (length >= 8) return 15;
        if (length >= 6) return 10;
        if (length >= 4) return 5;
        return 0;
    }

    private SpellPower classifyPower(int damage) {
        if (damage >= 36) return SpellPower.DEVASTATING;
        if (damage >= 21) return SpellPower.STRONG;
        if (damage >= 11) return SpellPower.MODERATE;
        return SpellPower.WEAK;
    }

    public String getSpellDescription() {
        return casterName + " cast \"" + word + "\" for " + totalDamage + " damage! [" + power + "]";
    }

    // Getters
    public String getWord() { return word; }
    public int getLetterValue() { return letterValue; }
    public int getWordLengthBonus() { return wordLengthBonus; }
    public int getTotalDamage() { return totalDamage; }
    public SpellPower getPower() { return power; }
    public String getCasterName() { return casterName; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return getSpellDescription();
    }
}
