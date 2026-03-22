package com.maxwell.translate_all_text_into_japanese.processor.lang;

import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class TranslationService {
    private static final Map<Resource, ResourceLocation> RESOURCE_LOCATIONS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void registerResource(Resource resource, ResourceLocation location) {
        if (resource != null && location != null) {
            RESOURCE_LOCATIONS.put(resource, location);
        }
    }

    public static ResourceLocation getResourceLocation(Resource resource) {
        return RESOURCE_LOCATIONS.get(resource);
    }

    /**
     * パスが翻訳対象のドキュメント（ガイド本等）であるか厳格に判定します。
     * システムファイル（モデル、レシピ、テクスチャ等）を誤って翻訳しないよう除外リストを持ちます。
     */
    public static boolean isTargetDocumentPath(String path) {
        if (path == null) return false;
        String p = path.toLowerCase();

        // 強力な除外リスト (これらが含まれるパスは翻訳対象外)
        if (p.contains("models/") || 
            p.contains("textures/") || 
            p.contains("blockstates/") || 
            p.contains("recipes/") || 
            p.contains("advancements/") || 
            p.contains("loot_tables/") || 
            p.contains("tags/") ||
            p.contains("lang/") ||    // 言語ファイル(既に別で処理済み)
            p.contains("font/") ||    // フォント
            p.contains("shaders/") || // シェーダー
            p.contains("particles/") || // パーティクル
            p.contains("sounds/") ||    // 音声
            p.contains("/item/") ||   // アイテム定義json
            p.contains("/block/")) {  // ブロック定義json
            return false;
        }

        // 包含リスト
        return p.contains("guide") || p.contains("patchouli") || p.contains("book") || 
               p.contains("malum") || p.contains("info") || p.contains("guideme");
    }

    public static String getTranslation(String text) {
        if (text == null || text.isEmpty()) return text;
        if (text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF].*")) return text;

        if (TranslationCache.contains(text)) {
            String cached = TranslationCache.get(text);
            if (cached != null) return cached;
        }

        TranslationDispatcher.submit(text, true);
        return "§e[待]§r " + text;
    }

    public static String getDocumentTranslation(String location, String content) {
        if (content == null || content.isEmpty()) return content;
        
        // ファイル単位でのキャッシュ読込・保存を廃止。
        // パラグラフ（行）単位でのキャッシュ引き当てに任せることで、一部でも翻訳が終わっていれば表示に即時反映させる。
        return MarkdownProcessor.process(content);
    }

    public static String getJsonTranslation(String location, String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) return jsonContent;
        
        // 同様に行・キー単位のプロセスへと委譲する。
        return JsonProcessor.process(jsonContent);
    }

    public static boolean isProbablyKey(String value) {
        if (value == null || value.isEmpty() || value.contains(" ")) return false;
        if (!value.matches("[a-zA-Z0-9_.:/-]+")) return false;
        return value.contains(".") || value.contains(":");
    }

    public static String applyJapaneseWordWrap(String text) {
        if (text == null) return null;
        // 英語のスペース文字を基準に改行する特殊な独自レンダラー（Malum等）向けに、
        // 句読点の後ろに半角スペースを強制挿入して自動改行を誘引する
        String spaced = text.replace("。", "。 ").replace("、", "、 ").replace("？", "？ ").replace("！", "！ ");
        
        // それでも長すぎる場合（15文字以上句読点が無い場合など）は強制的にスペースを挿入する
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < spaced.length(); i++) {
            char c = spaced.charAt(i);
            sb.append(c);
            
            if (c == ' ' || c == '\n') {
                count = 0;
            } else {
                count++;
                if (count >= 15) {
                    // 次の文字が禁則処理対象（句読点や閉じ括弧など）でない場合のみスペースを挿入
                    if (i + 1 < spaced.length()) {
                        char next = spaced.charAt(i + 1);
                        if ("。、！？）」』】".indexOf(next) == -1) {
                            sb.append(' ');
                            count = 0;
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
}