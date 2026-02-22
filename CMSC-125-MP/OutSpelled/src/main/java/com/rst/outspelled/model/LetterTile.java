package com.rst.outspelled.model;

import com.rst.outspelled.util.LetterValues;

public class LetterTile {

    public enum TileState {
        IDLE,       // default state
        SELECTED,   // player has clicked this tile
        USED        // tile has been used in a word
    }

    private char letter;
    private int value;
    private TileState state;
    private final int row;
    private final int col;

    public LetterTile(char letter, int row, int col) {
        this.letter = Character.toUpperCase(letter);
        this.value = LetterValues.getValue(letter);
        this.state = TileState.IDLE;
        this.row = row;
        this.col = col;
    }

    public void select() {
        if (state == TileState.IDLE) state = TileState.SELECTED;
    }

    public void deselect() {
        if (state == TileState.SELECTED) state = TileState.IDLE;
    }

    public void markUsed() {
        state = TileState.USED;
    }

    public void reset(char newLetter) {
        this.letter = Character.toUpperCase(newLetter);
        this.value = LetterValues.getValue(newLetter);
        this.state = TileState.IDLE;
    }

    public boolean isSelected() { return state == TileState.SELECTED; }
    public boolean isIdle() { return state == TileState.IDLE; }
    public boolean isUsed() { return state == TileState.USED; }

    // Getters
    public char getLetter() { return letter; }
    public int getValue() { return value; }
    public TileState getState() { return state; }
    public int getRow() { return row; }
    public int getCol() { return col; }

    @Override
    public String toString() {
        return String.valueOf(letter) + "(" + value + ")";
    }
}