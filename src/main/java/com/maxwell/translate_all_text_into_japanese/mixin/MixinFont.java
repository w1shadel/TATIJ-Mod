package com.maxwell.translate_all_text_into_japanese.mixin;

import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class MixinFont {
    @Shadow
    @Final
    private StringSplitter splitter;
    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true)
    private void onGetFormattedWidth(FormattedText text, CallbackInfoReturnable<Integer> cir) {
        String plainText = text.getString();
        if (TranslationCache.contains(plainText)) {
            cir.setReturnValue((int) this.splitter.stringWidth(TranslationCache.get(plainText)));
        }
    }
    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void onGetWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (text != null && TranslationCache.contains(text)) {
            String translated = TranslationCache.get(text);
            float width = this.splitter.stringWidth(translated);

            cir.setReturnValue((int) width);
        }
    }
}