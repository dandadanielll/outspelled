package com.rst.outspelled.ai;

import com.rst.outspelled.model.LetterGrid;
import java.util.Set;

public interface AiBrain {
    /*
     * Find the best word the AI can form from the given grid, excluding
     * already-used words.
     * Returns an empty string if no valid word is found
     */
    String findBestWord(LetterGrid grid, Set<String> usedWords);

    // Simulated thinking delay in milliseconds before the AI submits (varies with difficulty)
    long getThinkingDelayMs();
}
