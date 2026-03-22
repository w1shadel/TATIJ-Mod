package com.maxwell.translate_all_text_into_japanese.processor.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownProcessor {
    /**
     * Markdown形式のテキストを処理します。
     * 行単位で処理することで、レイアウト（改行、空白）を最大限に維持します。
     */
    public static String process(String content) {
        if (content == null || content.isEmpty()) return content;
        
        content = content.replace("\r\n", "\n");
        
        // フロントマター（YAML）のパターン
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
        String[] lines = body.split("\n", -1);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // 翻訳対象：空でない、記号だけでない、ある程度の長さがある
            if (shouldTranslate(trimmed)) {
                String translated = TranslationService.getTranslation(trimmed);
                sb.append(line.replace(trimmed, translated));
            } else {
                sb.append(line);
            }

            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static boolean shouldTranslate(String text) {
        if (text == null || text.length() < 2) return false;
        
        // 記号だけの行（水平線、リストの点、引用符など）は除外
        if (text.matches("^[\\s*\\-_#>]+$")) return false;
        
        // フロントマター風の行やタグ単体の行は除外
        if (text.startsWith("<") && text.endsWith(">")) return false;
        if (text.startsWith("---")) return false;

        // 日本語が含まれている場合はスキップ
        if (text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF].*")) return false;

        return true;
    }
}