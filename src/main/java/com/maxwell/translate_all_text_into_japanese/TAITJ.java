package com.maxwell.translate_all_text_into_japanese;

import com.maxwell.translate_all_text_into_japanese.processor.LanguageMapper;
import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TAITJ.MODID)
public class TAITJ
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "translate_all_text_into_japanese";
    public TAITJ(IEventBus modEventBus, ModContainer modContainer, Dist dist)
    {
        if (dist.isClient()) {
            TranslationCache.load();
            CompletableFuture.runAsync(LanguageMapper::seedFromAllLangFiles);
        }
    }
}
