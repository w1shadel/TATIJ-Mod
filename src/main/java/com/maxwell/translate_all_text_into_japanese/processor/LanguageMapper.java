package com.maxwell.translate_all_text_into_japanese.processor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maxwell.translate_all_text_into_japanese.util.AutoTransLog;
import com.maxwell.translate_all_text_into_japanese.processor.lang.TranslationDispatcher;
import com.maxwell.translate_all_text_into_japanese.processor.lang.TranslationService;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class LanguageMapper {

    public static void seedFromAllLangFiles() {
        var manager = Minecraft.getInstance().getResourceManager();
        int totalNamespaces = manager.getNamespaces().size();
        int scannedCount = 0;

        AutoTransLog.LOGGER.info("全Modの言語スキャンを開始します... (対象名前空間数: {})", totalNamespaces);

        for (String namespace : manager.getNamespaces()) {
            scanFile(namespace, "lang/en_us.json");
            scanFile(namespace, "lang/en_gb.json");
            scanFile(namespace, "lang/en_us.lang");
            
            scannedCount++;
        }
        AutoTransLog.LOGGER.info("全Modの言語クロスキャンの完了。完了した名前空間: {}/{}", scannedCount, totalNamespaces);
    }

    /**
     * 全Modのガイドブック関連フォルダ（guide, patchouli_books等）をスキャンし、事前に翻訳を開始します。
     */
    public static void seedFromAllGuides() {
        var manager = Minecraft.getInstance().getResourceManager();
        String[] guideDirs = {
            "guide", "guides", "patchouli_books", "book", "books", "guideme", 
            "info", "ae2guide", "lexicon", "manual", "tome", "journal", 
            "encyclopedia", "quests", "ftbquests"
        };
        
        AutoTransLog.LOGGER.info("ガイドブックの事前スキャンを開始します...");
        int count = 0;

        for (String dir : guideDirs) {
            Map<ResourceLocation, Resource> resources = manager.listResources(dir, rl -> 
                rl.getPath().endsWith(".md") || rl.getPath().endsWith(".json"));
            
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation rl = entry.getKey();
                String path = rl.getPath();
                
                // 翻訳対象のパスであれば処理
                if (TranslationService.isTargetDocumentPath(path)) {
                    try (var is = entry.getValue().open()) {
                        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        if (path.endsWith(".md")) {
                            TranslationService.getDocumentTranslation(rl.toString(), content);
                        } else {
                            TranslationService.getJsonTranslation(rl.toString(), content);
                        }
                        count++;
                    } catch (Exception ignored) {}
                }
            }
        }
        AutoTransLog.LOGGER.info("ガイドブックの事前スキャンが完了しました。対象ファイル数: {}", count);
    }

    private static void scanFile(String namespace, String path) {
        var manager = Minecraft.getInstance().getResourceManager();
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(namespace, path);

        manager.getResource(rl).ifPresent(resource -> {
            try (var is = resource.open()) {
                int count = 0;
                if (path.endsWith(".json")) {
                    try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        for (var entry : json.entrySet()) {
                            String value = entry.getValue().getAsString();
                            if (isTranslatable(value)) {
                                TranslationDispatcher.submit(value, false);
                                count++;
                            }
                        }
                    }
                } else if (path.endsWith(".lang")) {
                    Properties props = new Properties();
                    props.load(is);
                    for (String key : props.stringPropertyNames()) {
                        String value = props.getProperty(key);
                        if (isTranslatable(value)) {
                            TranslationDispatcher.submit(value, false);
                            count++;
                        }
                    }
                }
                
                if (count > 0) {
                    AutoTransLog.LOGGER.info("[AutoTrans] {}:{} から {} 件のテキストを追加しました。", namespace, path, count);
                }
            } catch (Exception ignored) {}
        });
    }

    private static boolean isTranslatable(String text) {
        if (text == null || text.length() < 2) return false;
        if (TranslationService.isProbablyKey(text)) return false;
        return text.matches(".*[a-zA-Z].*") && !text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF].*");
    }
}