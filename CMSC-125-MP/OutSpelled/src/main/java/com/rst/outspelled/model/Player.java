package com.rst.outspelled.model;

public class Player {
    private String name;
    private int hp;
    private int maxHp;
    private boolean isMyTurn;

    public Player(String name, int maxHp) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.isMyTurn = false;
    }

    public void takeDamage(int damage) {
        hp = Math.max(0, hp - damage);
    }

    public boolean isDefeated() {
        return hp <= 0;
    }

    public double getHpPercentage() {
        return (double) hp / maxHp;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = hp; }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public boolean isMyTurn() { return isMyTurn; }
    public void setMyTurn(boolean myTurn) { isMyTurn = myTurn; }

    @Override
    public String toString() {
        return name + " [HP: " + hp + "/" + maxHp + "]";
    }
}
