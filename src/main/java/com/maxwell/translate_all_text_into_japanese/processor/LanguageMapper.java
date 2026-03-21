package com.maxwell.translate_all_text_into_japanese.processor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maxwell.translate_all_text_into_japanese.util.AutoTransLog;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class LanguageMapper {
    public static void seedFromAllLangFiles() {
        var manager = Minecraft.getInstance().getResourceManager();

        for (String namespace : manager.getNamespaces()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(namespace, "lang/en_us.json");

            manager.getResource(rl).ifPresent(resource -> {
                try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                    int count = 0;
                    for (var entry : json.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue().getAsString();
                        if (!TranslationCache.contains(key)) {
                            TranslationCache.put(key, value);
                            count++;
                        }
                    }
                    if (count > 0) {
                        System.out.println("[AutoTrans] " + namespace + " から " + count + " 件の翻訳キーを辞書登録しました。");
                    }
                } catch (Exception e) {
                    AutoTransLog.LOGGER.error("The JSON file could not be opened.");
                }
            });
        }
        TranslationCache.save();
    }
}