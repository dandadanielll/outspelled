package com.rst.outspelled.dictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DictionaryLoader {

    private static final String DICTIONARY_PATH = "/dictionary.txt";
    private static Set<String> words = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean loaded = false;
    private static volatile boolean loading = false;

    public static void loadAsync(Runnable onComplete) {
        if (loaded || loading) return;
        loading = true;

        Thread loaderThread = new Thread(() -> {
            load();
            loaded = true;
            loading = false;
            if (onComplete != null) {
                onComplete.run();
            }
        });

        loaderThread.setName("DictionaryLoaderThread");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private static void load() {
        try (InputStream is = DictionaryLoader.class.getResourceAsStream(DICTIONARY_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim().toLowerCase());
            }

            System.out.println("Dictionary loaded: " + words.size() + " words.");

        } catch (IOException e) {
            System.err.println("Failed to load dictionary: " + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("Dictionary file not found at: " + DICTIONARY_PATH);
        }
    }

    public static Set<String> getWords() { return words; }
    public static boolean isLoaded() { return loaded; }
    public static boolean isLoading() { return loading; }
}