package com.maxwell.translate_all_text_into_japanese.processor.lang;



import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownProcessor {
    public static String process(String content) {
        content = content.replace("\r\n", "\n");
        // YAMLフロントマターの解析
        Pattern frontmatterPattern = Pattern.compile("^---\\n(.*?)\\n---(?:\\n|$)(.*)", Pattern.DOTALL);
        Matcher m = frontmatterPattern.matcher(content);

        if (m.matches()) {
            String yaml = m.group(1);
            String body = m.group(2);
            return "---\n" + yaml + "\n---\n" + processBody(body);
        }
        return processBody(content);
    }

    private static String processBody(String body) {
        String[] paragraphs = body.split("\\n\\s*\\n");
        StringBuilder sb = new StringBuilder();
        List<String> batch = new ArrayList<>();
        int chars = 0;

        for (String p : paragraphs) {
            batch.add(p);
            chars += p.length();
            if (chars > 2000) {
                sb.append(translateBatchDirectly(batch)).append("\n\n");
                batch.clear(); chars = 0;
            }
        }
        if (!batch.isEmpty()) sb.append(translateBatchDirectly(batch));
        return sb.toString().trim();
    }

    private static String translateBatchDirectly(List<String> batch) {
        String combined = String.join("\n\n", batch);
        // ドキュメント翻訳スレッド内なので、TagProtectorを直接呼んで翻訳を待つ
        return TagProtector.translateWithProtection(combined);
    }
}