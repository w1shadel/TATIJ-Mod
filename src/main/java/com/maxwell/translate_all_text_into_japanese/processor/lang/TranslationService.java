package com.maxwell.translate_all_text_into_japanese.processor.lang;

import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;

import java.util.concurrent.CompletableFuture;

public class TranslationService {

    public static String getTranslation(String text) {
        if (TranslationCache.contains(text)) return TranslationCache.get(text);
        TranslationDispatcher.submit(text, true);
        return "§e[待]§r " + text;
    }

    public static String getDocumentTranslation(String location, String content) {
        if (TranslationCache.contains(location)) return TranslationCache.get(location);

        CompletableFuture.runAsync(() -> {
            String translated = MarkdownProcessor.process(content);
            TranslationCache.put(location, translated);
            TranslationCache.save();
        });
        return content + "\n\n> **[AutoTrans] このページは現在裏で翻訳中です。完了後、F3+Tで反映されます。**\n";
    }

    public static String getJsonTranslation(String location, String jsonContent) {
        if (TranslationCache.contains(location)) return TranslationCache.get(location);

        CompletableFuture.runAsync(() -> {
            String translated = JsonProcessor.process(jsonContent);
            TranslationCache.put(location, translated);
            TranslationCache.save();
        });
        return jsonContent; // JSONは構造が壊れるため、翻訳中タグは一切入れない
    }
}