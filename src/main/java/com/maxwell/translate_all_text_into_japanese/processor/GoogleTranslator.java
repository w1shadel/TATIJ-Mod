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

    public static String fetchTranslation(String text) {
        try {
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=ja&dt=t&q="
                    + URLEncoder.encode(text, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                AutoTransLog.LOGGER.warn("API Rate Limit exceeded (429)! Waiting...");
                Thread.sleep(10000);
                return null;
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
                AutoTransLog.LOGGER.error("Google API Error: Status {}", response.statusCode());
            }
        } catch (Exception e) {
            AutoTransLog.LOGGER.error("API communication error {}", e.getMessage());
        }
        return text;
    }
}