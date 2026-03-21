package com.maxwell.translate_all_text_into_japanese.processor.lang;

import com.google.gson.*;

import java.util.Arrays;
import java.util.Map;

public class JsonProcessor {
    public static String process(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            return new GsonBuilder().setPrettyPrinting().create().toJson(translateElement(root));
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

                if (isStructuralKey(key)) {
                    newObj.add(key, val);
                } else if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                    newObj.addProperty(key, TagProtector.translateWithProtection(val.getAsString()));
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

    private static boolean isStructuralKey(String key) {
        return Arrays.asList("type", "category", "icon", "spell", "advancement", "id", "key", "modName", "modVersion").contains(key);
    }
}