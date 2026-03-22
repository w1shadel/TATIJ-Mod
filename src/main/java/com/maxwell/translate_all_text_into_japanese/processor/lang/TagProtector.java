package com.maxwell.translate_all_text_into_japanese.processor.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagProtector {
    private static final Pattern PROTECT_PATTERN = Pattern.compile("(?s)<[^>]+?>|§[0-9a-fklmnor]");

    public static class ProtectionResult {
        public final String protectedText;
        public final List<String> tags;

        public ProtectionResult(String protectedText, List<String> tags) {
            this.protectedText = protectedText;
            this.tags = tags;
        }
    }

    public static ProtectionResult protect(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ProtectionResult(text, new ArrayList<>());
        }

        List<String> tags = new ArrayList<>();
        Matcher matcher = PROTECT_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int i = 0;

        while (matcher.find()) {
            tags.add(matcher.group());
            matcher.appendReplacement(sb, "[##" + (i++) + "##]");
        }
        matcher.appendTail(sb);

        return new ProtectionResult(sb.toString(), tags);
    }

    public static String restore(String translated, List<String> tags) {
        if (translated == null || tags == null || tags.isEmpty()) {
            return translated;
        }

        for (int i = 0; i < tags.size(); i++) {
            String regex = "\\[\\s*##\\s*" + i + "\\s*##\\s*\\]";
            translated = translated.replaceAll(regex, Matcher.quoteReplacement(tags.get(i)));
        }
        return translated;
    }
}