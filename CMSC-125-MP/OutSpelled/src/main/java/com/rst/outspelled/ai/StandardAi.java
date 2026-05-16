package com.rst.outspelled.ai;

import com.rst.outspelled.dictionary.WordValidator;
import com.rst.outspelled.model.LetterGrid;
import com.rst.outspelled.model.LetterTile;
import com.rst.outspelled.util.LetterValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/* Standard difficulty AI (dagdag pako ng ez and hard pa mamaya) 
   Searches for words up to 6 letters, picks the highest scoring one,
   and waits 1.5-2.5 seconds before submitting to simulate thinking */
class StandardAi implements AiBrain {

    private final Random random = new Random();

    @Override
    public String findBestWord(LetterGrid grid) {
        List<String> validWords = new ArrayList<>(); //filled with valid words the ai finds 
        LetterTile[][] tiles = grid.getGrid(); //gets the raw 4x4 2D array of tiles
        boolean[] used = new boolean[16]; // tracks which of the 16 tiles are already in the current word

        findWords(tiles, used, new StringBuilder(), validWords, 3, 6); //hard coded to only use up to 6 letter words for now

        if (validWords.isEmpty()) return ""; //returns empty string if no valid words found

        // pick the word with the highest letter-value score (greedy selection, not really optimized but works for now)
        return validWords.stream()
                .max((a, b) -> LetterValues.getWordValue(a) - LetterValues.getWordValue(b))
                .orElse("");
    }

    /* Recursive backtracking search over the grid
       Tries every combination of idle tiles to build words between minLen and maxLen */
    private void findWords(LetterTile[][] tiles, boolean[] used, StringBuilder current, List<String> results, int minLen, int maxLen) {

        if (current.length() >= minLen) { //word is long enough to be considered
            String word = current.toString().toLowerCase();
            if (WordValidator.validateSync(word) && !results.contains(word)) { //checks if the word is valid and not already found
                results.add(word);
            }
        }

        if (current.length() >= maxLen) return; //stop going deeper once max length reached

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                int idx = r * 4 + c;
                if (!used[idx] && tiles[r][c].isIdle()) {
                    used[idx] = true;
                    current.append(tiles[r][c].getLetter());

                    findWords(tiles, used, current, results, minLen, maxLen);

                    // backtrack: undo the last letter and mark tile as unused again
                    current.deleteCharAt(current.length() - 1);
                    used[idx] = false;
                }
            }
        }
    }

    @Override
    public long getThinkingDelayMs() {
        return 1500 + random.nextInt(1000); // 1.5 to 2.5 seconds
    }
}
