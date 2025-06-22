package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TropicalFishEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void makeSpecialTropicalFishGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFishEntity tropicalFish) {
            TropicalFishEntity.Variant currentVariant = new TropicalFishEntity.Variant(
                    tropicalFish.getVariant(),
                    tropicalFish.getBaseColorComponents(),
                    tropicalFish.getPatternColorComponents()
            );

            if (!TropicalFishEntity.COMMON_VARIANTS.contains(currentVariant)) {
                cir.setReturnValue(TropicalFishConfig.get().glowEnabled);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void handleSpecialTropicalFishNames(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFishEntity tropicalFish) {
            TropicalFishEntity.Variant currentVariant = new TropicalFishEntity.Variant(
                    tropicalFish.getVariant(),
                    tropicalFish.getBaseColorComponents(),
                    tropicalFish.getPatternColorComponents()
            );

            if (!TropicalFishEntity.COMMON_VARIANTS.contains(currentVariant)) {
                TropicalFishConfig config = TropicalFishConfig.get();

                if (config.namesEnabled) {
                    if (!tropicalFish.hasCustomName()) {
                        String patternName = tropicalFish.getVariant().asString();  // Changed
                        String baseColorName = tropicalFish.getBaseColorComponents().asString().replace('_', ' ');  // Changed
                        String patternColorName = tropicalFish.getPatternColorComponents().asString().replace('_', ' ');  // Changed

                        Text patternText = Text.literal(patternName).formatted(Formatting.GOLD, Formatting.BOLD);

                        Text colorText;
                        if (tropicalFish.getBaseColorComponents() == tropicalFish.getPatternColorComponents()) {  // Changed
                            colorText = Text.literal("solid " + baseColorName)
                                    .formatted(Formatting.LIGHT_PURPLE);
                        } else {
                            colorText = Text.literal(baseColorName + " & " + patternColorName)
                                    .formatted(Formatting.YELLOW);
                        }

                        Text customName = Text.empty()
                                .append(patternText)
                                .append(Text.literal(" ").formatted(Formatting.RESET))
                                .append(colorText);

                        tropicalFish.setCustomName(customName);
                        tropicalFish.setCustomNameVisible(true);
                    }
                } else {
                    if (tropicalFish.hasCustomName()) {
                        tropicalFish.setCustomName(null);
                        tropicalFish.setCustomNameVisible(false);
                    }
                }
            }
        }
    }
}