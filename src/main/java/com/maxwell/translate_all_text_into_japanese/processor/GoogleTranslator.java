package com.maxwell.translate_all_text_into_japanese.processor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.maxwell.translate_all_text_into_japanese.util.AutoTransLog;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GoogleTranslator {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Google翻訳APIを使用してテキストを翻訳します。
     * ネットワークエラーやレート制限(429)が発生した場合は null を返し、呼び出し側にリトライを促します。
     */
    public static String fetchTranslation(String text) {
        if (text == null || text.isEmpty()) return text;
        
        try {
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ja&dt=t&q="
                    + URLEncoder.encode(text, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 429 || response.statusCode() == 302) {
                AutoTransLog.LOGGER.warn("API Rate Limit or Blocked ({})! Waiting 10s... [Text: {}]", response.statusCode(), truncate(text, 20));
                Thread.sleep(10000);
                return null; // リトライ対象
            }
            
            if (response.statusCode() == 200) {
                JsonArray jsonResponse = JsonParser.parseString(response.body()).getAsJsonArray();
                StringBuilder result = new StringBuilder();
                JsonArray segments = jsonResponse.get(0).getAsJsonArray();
                for (JsonElement segment : segments) {
                    if (segment.isJsonArray()) {
                        JsonElement textNode = segment.getAsJsonArray().get(0);
                        if (textNode != null && !textNode.isJsonNull()) {
                            result.append(textNode.getAsString());
                        }
                    }
                }
                return result.toString();
            } else {
                AutoTransLog.LOGGER.error("Google API Error: Status {} [Text: {}]", response.statusCode(), truncate(text, 20));
                // 400系などの致命的なエラーの場合はリトライせず原文を返す
                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    return text;
                }
                return null; // 500系などはリトライ対象
            }
        } catch (Exception e) {
            AutoTransLog.LOGGER.error("API communication error: {} [Text: {}]", e.getMessage(), truncate(text, 20));
            return null; // ネットワークエラー等はリトライ対象
        }
    }

    private static String truncate(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }
}