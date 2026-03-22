package com.maxwell.translate_all_text_into_japanese.mixin;

import com.maxwell.translate_all_text_into_japanese.processor.lang.TranslationService;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public class MixinLanguage {
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void onGetOrDefault(String key, String defaultValue, CallbackInfoReturnable<String> cir) {
        if (IS_PROCESSING.get()) return;
        IS_PROCESSING.set(true);
        try {
            String originalResult = cir.getReturnValue();

            if (originalResult == null || originalResult.equals(key)) return;
            if (originalResult.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF].*")) return;
            if (!originalResult.matches(".*[a-zA-Z].*")) return;

            String translated = TranslationService.getTranslation(originalResult);
            if (!translated.equals(originalResult)) {
                if (key.startsWith("malum.gui.book")) {
                    translated = TranslationService.applyJapaneseWordWrap(translated);
                }
                cir.setReturnValue(translated);
            }
        } finally {
            IS_PROCESSING.set(false);
        }
    }
}