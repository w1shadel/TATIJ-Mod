package com.maxwell.translate_all_text_into_japanese;

import com.maxwell.translate_all_text_into_japanese.processor.LanguageMapper;
import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod(TAITJ.MODID)
public class TAITJ
{
    public static final String MODID = "translate_all_text_into_japanese";
    public TAITJ(IEventBus modEventBus, ModContainer modContainer, Dist dist)
    {
        if (dist.isClient()) {
            TranslationCache.load();
            
            // 起動時に言語ファイルとガイドブックをバックグラウンドで事前スキャン
            CompletableFuture.runAsync(() -> {
                LanguageMapper.seedFromAllLangFiles();
                LanguageMapper.seedFromAllGuides();
            });
        }
    }
}
