package com.maxwell.translate_all_text_into_japanese.processor.lang;

import com.maxwell.translate_all_text_into_japanese.util.AutoTransLog;
import com.maxwell.translate_all_text_into_japanese.processor.GoogleTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagProtector {
    private static final Pattern PROTECT_PATTERN = Pattern.compile("<[^>]+?/>|<[^>]+>");

    public static String translateWithProtection(String text) {
        if (text.trim().isEmpty()) return text;

        List<String> tags = new ArrayList<>();
        Matcher matcher = PROTECT_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int i = 0;

        while (matcher.find()) {
            tags.add(matcher.group());
            matcher.appendReplacement(sb, "[##" + (i++) + "##]");
        }
        matcher.appendTail(sb);

        String translated = GoogleTranslator.fetchTranslation(sb.toString());

        return restoreTags(translated, tags);
    }

    private static String restoreTags(String translated, List<String> tags) {
        for (int i = 0; i < tags.size(); i++) {
            String regex = "\\[\\s*##\\s*" + i + "\\s*##\\s*\\]";
            translated = translated.replaceAll(regex, Matcher.quoteReplacement(tags.get(i)));
        }
        if (translated.contains("[##")) {
            AutoTransLog.LOGGER.error("Warning: Restoration failed. Some tags may not have been restored. {}", translated);
        }

        return translated;
    }
}