package com.rst.outspelled.util;

import java.util.HashMap;
import java.util.Map;

public class LetterValues {

    private static final Map<Character, Integer> VALUES = new HashMap<>();

    static {
        // 1 point
        for (char c : "aeioulnstrAEIOULNSTR".toCharArray()) VALUES.put(c, 1);
        // 2 points
        for (char c : "dgDG".toCharArray()) VALUES.put(c, 2);
        // 3 points
        for (char c : "bcmpBCMP".toCharArray()) VALUES.put(c, 3);
        // 4 points
        for (char c : "fhvwyFHVWY".toCharArray()) VALUES.put(c, 4);
        // 5 points
        for (char c : "kK".toCharArray()) VALUES.put(c, 5);
        // 8 points
        for (char c : "jxJX".toCharArray()) VALUES.put(c, 8);
        // 10 points
        for (char c : "qzQZ".toCharArray()) VALUES.put(c, 10);
    }

    public static int getValue(char c) {
        return VALUES.getOrDefault(Character.toLowerCase(c), 0);
    }

    public static int getWordValue(String word) {
        int total = 0;
        for (char c : word.toCharArray()) {
            total += getValue(c);
        }
        return total;
    }
}