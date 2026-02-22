package com.rst.outspelled.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LetterGrid {

    // Weighted letter pool — more common letters appear more often
    private static final String LETTER_POOL =
            "AAAAAAAAABBBCCCDDDDEEEEEEEEEEFFFGGGHHHHIIIIIIIIJKLLLL" +
                    "MMMNNNNNNNOOOOOOOOPPQRRRRRSSSSTTTTTTTUUUUVVWWXYYZ";

    private static final int ROWS = 4;
    private static final int COLS = 4;

    private final LetterTile[][] grid;
    private final List<LetterTile> selectedTiles;
    private final Random random;

    public LetterGrid() {
        this(new Random());
    }

    /** Create grid with given seed so both clients can have identical layout. */
    public LetterGrid(long seed) {
        this(new Random(seed));
    }

    private LetterGrid(Random r) {
        this.grid = new LetterTile[ROWS][COLS];
        this.selectedTiles = new ArrayList<>();
        this.random = r;
        generateGrid();
    }

    private void generateGrid() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = new LetterTile(randomLetter(), r, c);
            }
        }
    }

    private char randomLetter() {
        return LETTER_POOL.charAt(random.nextInt(LETTER_POOL.length()));
    }

    public boolean selectTile(int row, int col) {
        LetterTile tile = grid[row][col];
        if (!tile.isIdle()) return false;
        tile.select();
        selectedTiles.add(tile);
        return true;
    }

    public boolean deselectTile(int row, int col) {
        LetterTile tile = grid[row][col];
        if (!tile.isSelected()) return false;
        tile.deselect();
        selectedTiles.remove(tile);
        return true;
    }

    public void deselectAll() {
        for (LetterTile tile : new ArrayList<>(selectedTiles)) {
            tile.deselect();
        }
        selectedTiles.clear();
    }

    public String getSelectedWord() {
        StringBuilder sb = new StringBuilder();
        for (LetterTile tile : selectedTiles) {
            sb.append(tile.getLetter());
        }
        return sb.toString();
    }

    public void confirmWord() {
        for (LetterTile tile : new ArrayList<>(selectedTiles)) {
            tile.markUsed();
        }
        replaceUsedTiles();
        selectedTiles.clear();
    }

    /** Regenerate entire grid (full reset, not just replace used tiles). */
    public void resetEntireGrid() {
        deselectAll();
        generateGrid();
    }

    /** Reset grid using given seed so both clients stay in sync. */
    public void resetWithSeed(long seed) {
        this.random.setSeed(seed);
        resetEntireGrid();
    }

    private void replaceUsedTiles() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c].isUsed()) {
                    grid[r][c].reset(randomLetter());
                }
            }
        }
    }

    /** Shuffle all tiles (clears selection). */
    public void shuffleGrid() {
        deselectAll();
        shuffleIdleTilesOnly();
    }

    /** Shuffle only non-selected (idle) tiles; selected letters stay in place. */
    public void shuffleIdleTilesOnly() {
        List<Character> idleLetters = new ArrayList<>();
        List<int[]> idlePositions = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c].isIdle()) {
                    idleLetters.add(grid[r][c].getLetter());
                    idlePositions.add(new int[]{r, c});
                }
            }
        }
        Collections.shuffle(idleLetters);
        for (int i = 0; i < idleLetters.size(); i++) {
            int[] pos = idlePositions.get(i);
            grid[pos[0]][pos[1]].reset(idleLetters.get(i));
        }
    }

    /** Returns 16 letters in row-major order (for network sync). */
    public String getLettersAsString() {
        StringBuilder sb = new StringBuilder(16);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sb.append(grid[r][c].getLetter());
            }
        }
        return sb.toString();
    }

    /** Apply 16 letters in row-major order (from network). */
    public void applyLayout(String letters) {
        if (letters == null || letters.length() < 16) return;
        List<Character> list = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            list.add(Character.toUpperCase(letters.charAt(i)));
        }
        applyLayout(list);
    }

    private void applyLayout(List<Character> letters) {
        int index = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c].reset(letters.get(index++));
            }
        }
    }

    public LetterTile selectFirstMatchingTile(char letter) {
        char upper = Character.toUpperCase(letter);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c].getLetter() == upper && grid[r][c].isIdle()) {
                    grid[r][c].select();
                    selectedTiles.add(grid[r][c]);
                    return grid[r][c];
                }
            }
        }
        return null; // no matching idle tile found
    }

    public LetterTile deselectLastMatchingTile(char letter) {
        char upper = Character.toUpperCase(letter);
        // search from end of selected list for backspace support
        for (int i = selectedTiles.size() - 1; i >= 0; i--) {
            if (selectedTiles.get(i).getLetter() == upper) {
                LetterTile tile = selectedTiles.get(i);
                tile.deselect();
                selectedTiles.remove(i);
                return tile;
            }
        }
        return null;
    }

    // Getters
    public LetterTile getTile(int row, int col) { return grid[row][col]; }
    public List<LetterTile> getSelectedTiles() { return Collections.unmodifiableList(selectedTiles); }
    public int getRows() { return ROWS; }
    public int getCols() { return COLS; }
    public LetterTile[][] getGrid() { return grid; }
}