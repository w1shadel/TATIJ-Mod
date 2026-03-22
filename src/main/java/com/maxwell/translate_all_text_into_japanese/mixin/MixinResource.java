package com.maxwell.translate_all_text_into_japanese.mixin;

import com.maxwell.translate_all_text_into_japanese.processor.lang.TranslationService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Mixin(Resource.class)
public class MixinResource {
    /**
     * Resource.open() が呼ばれた際に、対象のドキュメント（ガイド本等）であれば
     * 中身を日本語に翻訳した Stream に差し替える。
     */
    @Inject(method = "open", at = @At("RETURN"), cancellable = true)
    private void onOpen(CallbackInfoReturnable<InputStream> cir) {
        Resource self = (Resource)(Object)this;
        ResourceLocation location = TranslationService.getResourceLocation(self);
        
        if (location == null) return;
        
        String path = location.getPath();
        if (!TranslationService.isTargetDocumentPath(path)) return;

        try {
            InputStream originalStream = cir.getReturnValue();
            if (originalStream == null) return;

            // 重要: InputStream を一生分読み込むと、呼び出し側が読めなくなってしまう。
            // そのため、読み込んだ後は必ず新しい ByteArrayInputStream として作り直して返す。
            byte[] allBytes = originalStream.readAllBytes();
            String originalText = new String(allBytes, StandardCharsets.UTF_8);
            String translatedText;

            if (path.endsWith(".md")) {
                translatedText = TranslationService.getDocumentTranslation(location.toString(), originalText);
            } else if (path.endsWith(".json")) {
                translatedText = TranslationService.getJsonTranslation(location.toString(), originalText);
            } else {
                // 対象外の拡張子の場合は、読み込んだバイトデータをそのまま新しいストリームとして返す
                cir.setReturnValue(new ByteArrayInputStream(allBytes));
                return;
            }

            // 翻訳されたかどうかにかかわらず、読み込み済みのバイトデータを新しいストリームとしてセットする。
            // これにより呼び出し側（Mod）は最初からデータを読み取ることができる。
            byte[] finalBytes = translatedText.equals(originalText) ? allBytes : translatedText.getBytes(StandardCharsets.UTF_8);
            cir.setReturnValue(new ByteArrayInputStream(finalBytes));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
