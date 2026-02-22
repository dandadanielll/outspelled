package com.rst.outspelled.dictionary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WordValidator {

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("WordValidatorThread");
                t.setDaemon(true);
                return t;
            });

    public static Future<Boolean> validateAsync(String word) {
        return executor.submit(() -> {
            if (word == null || word.isBlank()) return false;
            if (!DictionaryLoader.isLoaded()) return false;
            if (word.length() < 3) return false;
            return DictionaryLoader.getWords().contains(word.trim().toLowerCase());
        });
    }

    public static boolean validateSync(String word) {
        if (word == null || word.isBlank()) return false;
        if (!DictionaryLoader.isLoaded()) return false;
        if (word.length() < 3) return false;
        return DictionaryLoader.getWords().contains(word.trim().toLowerCase());
    }

    public static void validateAsync(String word, ValidationCallback callback) {
        executor.submit(() -> {
            boolean valid = validateSync(word);
            callback.onResult(valid);
        });
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public interface ValidationCallback {
        void onResult(boolean isValid);
    }
}