package com.rst.outspelled.dictionary;

import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.hunspell.WordFormGenerator;
import org.apache.lucene.analysis.hunspell.AffixedWord;
import org.apache.lucene.store.ByteBuffersDirectory;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DictionaryLoader {

    private static final String AFFIX_PATH = "/en_US.aff";
    private static final String DICT_PATH = "/en_US.dic";
    private static final Set<String> words = Collections.synchronizedSet(new HashSet<>());
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
        try (InputStream affStream = DictionaryLoader.class.getResourceAsStream(AFFIX_PATH);
             InputStream dicStream = DictionaryLoader.class.getResourceAsStream(DICT_PATH)) {

            if (affStream == null || dicStream == null) {
                System.err.println("Dictionary files not found at: " + AFFIX_PATH + " or " + DICT_PATH);
                return;
            }

            Dictionary hunspellDict = new Dictionary(new ByteBuffersDirectory(), "temp", affStream, dicStream);
            WordFormGenerator generator = new WordFormGenerator(hunspellDict);

            // Read the dictionary root words
            // In Hunspell, the .dic file has the roots. We can just parse the .dic manually or use WordFormGenerator?
            // Actually, to expand all words in the dictionary, we should read the .dic file and pass each root to generator.getAllWordForms()
            // Wait! The user says "Expand all word forms from both files into a HashSet<String> at startup".
            // We can read the .dic file (ignoring the first line which is the count), split by "/" to get the root, and generate word forms.
            // But wait, WordFormGenerator can generate forms from a root. We need to pass the root.
            
            // Re-open dicStream to read roots manually? Yes, because lucene's Dictionary class parses it but doesn't expose an easy way to iterate all roots.
            try (InputStream dicReaderStream = DictionaryLoader.class.getResourceAsStream(DICT_PATH);
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(dicReaderStream))) {
                
                String line = reader.readLine(); // skip first line (count)
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String root = line.split("/")[0].trim();
                    List<AffixedWord> forms = generator.getAllWordForms(root, () -> {});
                    for (AffixedWord form : forms) {
                        words.add(form.getWord().toLowerCase());
                    }
                }
            }
            
            System.out.println("Dictionary loaded: " + words.size() + " expanded words.");

        } catch (Exception e) {
            System.err.println("Failed to load dictionary: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Set<String> getWords() { return words; }
    public static boolean isLoaded() { return loaded; }
    public static boolean isLoading() { return loading; }
}