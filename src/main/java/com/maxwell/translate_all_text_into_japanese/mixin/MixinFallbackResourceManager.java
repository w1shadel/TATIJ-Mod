package com.maxwell.translate_all_text_into_japanese.mixin;

import com.maxwell.translate_all_text_into_japanese.processor.lang.TranslationService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(FallbackResourceManager.class)
public class MixinFallbackResourceManager {
    @Inject(method = "getResource", at = @At("RETURN"))
    private void onGetResource(ResourceLocation location, CallbackInfoReturnable<Optional<Resource>> cir) {
        Optional<Resource> res = cir.getReturnValue();
        if (res != null && res.isPresent()) {
            TranslationService.registerResource(res.get(), location);
        }
    }

    @Inject(method = "listResources", at = @At("RETURN"))
    private void onListResources(String path, Predicate<ResourceLocation> filter, CallbackInfoReturnable<Map<ResourceLocation, Resource>> cir) {
        Map<ResourceLocation, Resource> map = cir.getReturnValue();
        if (map != null) {
            for (Map.Entry<ResourceLocation, Resource> entry : map.entrySet()) {
                TranslationService.registerResource(entry.getValue(), entry.getKey());
            }
        }
    }
}
