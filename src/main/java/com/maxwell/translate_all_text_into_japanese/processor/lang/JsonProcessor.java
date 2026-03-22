package com.maxwell.translate_all_text_into_japanese.processor.lang;

import com.google.gson.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JsonProcessor {
    // 翻訳対象外とする構造的なキーのリスト
    private static final List<String> STRUCTURAL_KEYS = Arrays.asList(
            "type", "category", "icon", "spell", "advancement", "id", "key", "modName", "modVersion", 
            "uuid", "entity", "name", "recipe", "atlas", "parent", "requirement", "item", "block", 
            "texture", "resource", "url", "link", "site", "goal", "entry", "flag", "page",
            "processor", "group", "template", "component", "components"
    );

    /**
     * JSON形式のテキストを解析し、翻訳対象の文字列を翻訳キューに登録して、結果のJSONを返します。
     * すでに翻訳済みの値があればそれを使用します。
     */
    public static String process(String json) {
        if (json == null || json.isEmpty()) return json;
        try {
            JsonElement root = JsonParser.parseString(json);
            return new GsonBuilder().disableHtmlEscaping().create().toJson(translateElement(root));
        } catch (Exception e) {
            return json;
        }
    }

    private static JsonElement translateElement(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonObject newObj = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement val = entry.getValue();

                if (STRUCTURAL_KEYS.contains(key)) {
                    newObj.add(key, val);
                } else if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                    String strVal = val.getAsString();
                    
                    // 翻訳キー（ドット区切り）やリソースロケーション（コロン区切り）と思われる場合は翻訳しない
                    if (TranslationService.isProbablyKey(strVal)) {
                        newObj.addProperty(key, strVal);
                    } else {
                        // 複数行にまたがる値を適切に行単位で分割して翻訳・結合する
                        newObj.addProperty(key, processMultilineString(strVal));
                    }
                } else {
                    newObj.add(key, translateElement(val));
                }
            }
            return newObj;
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            JsonArray newArr = new JsonArray();
            for (JsonElement e : arr) {
                newArr.add(translateElement(e));
            }
            return newArr;
        }
        return element;
    }

    private static String processMultilineString(String strVal) {
        if (!strVal.contains("\n")) {
            return TranslationService.getTranslation(strVal);
        }
        
        String cleanStr = strVal.replace("\r\n", "\n");
        String[] lines = cleanStr.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            
            // 翻訳対象の行かチェック
            if (!trimmed.isEmpty() && !TranslationService.isProbablyKey(trimmed)) {
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
}