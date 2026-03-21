package com.maxwell.translate_all_text_into_japanese.processor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_PATH = FMLPaths.CONFIGDIR.get().resolve("autotrans_cache.json");
    private static final Map<String, String> cacheMap = new ConcurrentHashMap<>();

    public static void load() {
        if (!Files.exists(CACHE_PATH)) return;

        try (Reader reader = Files.newBufferedReader(CACHE_PATH, StandardCharsets.UTF_8)) {
            Map<String, String> loaded = GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            if (loaded != null) {
                cacheMap.putAll(loaded);
                System.out.println("[AutoTrans] Cache loaded: " + cacheMap.size() + " entries.");
            }
        } catch (Exception e) {
            System.err.println("[AutoTrans] Failed to load cache: " + e.getMessage());
        }
    }
    public static synchronized void save() {
        try {
            Files.createDirectories(CACHE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CACHE_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(cacheMap, writer);
            }
        } catch (Exception e) {
            System.err.println("[AutoTrans] Failed to save cache: " + e.getMessage());
        }
    }

    public static String get(String english) {
        return cacheMap.get(english);
    }

    public static void put(String english, String japanese) {
        cacheMap.put(english, japanese);
    }

    public static boolean contains(String english) {
        return cacheMap.containsKey(english);
    }
}