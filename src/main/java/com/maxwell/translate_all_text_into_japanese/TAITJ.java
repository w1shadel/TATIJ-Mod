package com.maxwell.translate_all_text_into_japanese;

import com.maxwell.translate_all_text_into_japanese.processor.LanguageMapper;
import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;
import com.maxwell.translate_all_text_into_japanese.processor.lang.TranslationDispatcher;
import com.maxwell.translate_all_text_into_japanese.processor.lang.TranslationService;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod(TAITJ.MODID)
public class TAITJ
{
    public static final String MODID = "taitj";
    public TAITJ(IEventBus modEventBus, ModContainer modContainer, Dist dist)
    {
        if (dist.isClient()) {
            TranslationCache.load();
            
            // 起動時の事前スキャンを再有効化
            modEventBus.addListener(this::onLoadComplete);
        }
    }

    private void onLoadComplete(final net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent event) {
        // すべてのModロードが完了した安全なタイミングで翻訳エンジンを起動する
        TranslationService.init();
        TranslationDispatcher.init();
        
        CompletableFuture.runAsync(() -> {
            LanguageMapper.seedFromAllLangFiles();
            LanguageMapper.seedFromAllGuides();
        });
    }
}
