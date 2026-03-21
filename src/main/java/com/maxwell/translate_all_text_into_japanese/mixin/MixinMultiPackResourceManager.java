package com.maxwell.translate_all_text_into_japanese.mixin;

import com.maxwell.translate_all_text_into_japanese.processor.lang.TranslationService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(MultiPackResourceManager.class)
public class MixinMultiPackResourceManager {
    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void onGetResource(ResourceLocation location, CallbackInfoReturnable<Optional<Resource>> cir) {
        Optional<Resource> processed = processResource(location, cir.getReturnValue());
        if (processed != null) cir.setReturnValue(processed);
    }

    @Inject(method = "listResources", at = @At("RETURN"), cancellable = true)
    private void onListResources(String path, Predicate<ResourceLocation> filter, CallbackInfoReturnable<Map<ResourceLocation, Resource>> cir) {
        Map<ResourceLocation, Resource> originalMap = cir.getReturnValue();
        if (originalMap == null || originalMap.isEmpty()) return;

        Map<ResourceLocation, Resource> newMap = new HashMap<>();
        boolean hasModified = false;
        for (Map.Entry<ResourceLocation, Resource> entry : originalMap.entrySet()) {
            Optional<Resource> processed = processResource(entry.getKey(), Optional.of(entry.getValue()));
            if (processed != null && processed.isPresent()) {
                newMap.put(entry.getKey(), processed.get());
                hasModified = true;
            } else {
                newMap.put(entry.getKey(), entry.getValue());
            }
        }
        if (hasModified) cir.setReturnValue(newMap);
    }

    private Optional<Resource> processResource(ResourceLocation location, Optional<Resource> originalOpt) {
        if (originalOpt == null || originalOpt.isEmpty()) return null;

        String path = location.getPath();
        if (path.contains("patchouli_books")) return null;
        if (!path.contains("ae2guide")) return null;

        try {
            Resource resource = originalOpt.get();
            String originalText;
            try (InputStream is = resource.open()) {
                originalText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            String translatedText;
            if (path.endsWith(".md")) {
                translatedText = TranslationService.getDocumentTranslation(location.toString(), originalText);
            } else if (path.endsWith(".json")) {
                translatedText = TranslationService.getJsonTranslation(location.toString(), originalText);
            } else {
                return null;
            }

            if (translatedText.equals(originalText)) return null;

            byte[] bytes = translatedText.getBytes(StandardCharsets.UTF_8);
            return Optional.of(new Resource(resource.source(), () -> new ByteArrayInputStream(bytes)));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}