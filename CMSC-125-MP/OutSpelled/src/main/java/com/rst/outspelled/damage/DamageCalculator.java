package com.rst.outspelled.damage;

import com.rst.outspelled.model.Spell;
import com.rst.outspelled.model.Player;

public class DamageCalculator {

    // Base multiplier for all spells
    private static final double BASE_MULTIPLIER = 1.0;

    public static int calculate(Spell spell) {
        return spell.getTotalDamage();
    }

    public static int calculateWithMultiplier(Spell spell, double multiplier) {
        return (int) Math.round(spell.getTotalDamage() * multiplier);
    }

    public static void applyDamage(Spell spell, Player target) {
        int damage = calculate(spell);
        target.takeDamage(damage);
    }

    public static void applyDamageWithMultiplier(Spell spell, Player target, double multiplier) {
        int damage = calculateWithMultiplier(spell, multiplier);
        target.takeDamage(damage);
    }

    public static String getDamageReport(Spell spell, Player target, int previousHp) {
        int damage = calculate(spell);
        return target.getName() + " took " + damage + " damage! "
                + "(" + previousHp + " → " + target.getHp() + " HP)";
    }
}