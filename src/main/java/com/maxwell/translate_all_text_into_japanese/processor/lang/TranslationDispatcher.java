package com.maxwell.translate_all_text_into_japanese.processor.lang;

import com.maxwell.translate_all_text_into_japanese.processor.GoogleTranslator;
import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TranslationDispatcher {
    private static final BlockingQueue<String> URGENT = new LinkedBlockingQueue<>();
    private static final BlockingQueue<String> BACKGROUND = new LinkedBlockingQueue<>();

    static {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    String text = !URGENT.isEmpty() ? URGENT.take() : BACKGROUND.take();
                    String translated = GoogleTranslator.fetchTranslation(text);
                    if (translated != null && !translated.isEmpty()) {
                        TranslationCache.put(text, translated);
                        TranslationCache.save();
                    }
                    Thread.sleep(800);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    public static void submit(String text, boolean urgent) {
        if (TranslationCache.contains(text)) return;
        if (urgent) URGENT.offer(text);
        else BACKGROUND.offer(text);
    }
}