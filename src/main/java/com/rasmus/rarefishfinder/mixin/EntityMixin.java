package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void makeSpecialTropicalFishGlow(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFish tropicalFish) {
            TropicalFish.Variant currentVariant = new TropicalFish.Variant(
                    tropicalFish.getPattern(),
                    tropicalFish.getBaseColor(),
                    tropicalFish.getPatternColor()
            );

            if (!TropicalFish.COMMON_VARIANTS.contains(currentVariant)) {
                cir.setReturnValue(TropicalFishConfig.get().glowEnabled);
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void useConfiguredGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFish tropicalFish) {
            TropicalFishConfig config = TropicalFishConfig.get();
            if (!config.glowEnabled) {
                return;
            }

            TropicalFish.Variant currentVariant = new TropicalFish.Variant(
                    tropicalFish.getPattern(),
                    tropicalFish.getBaseColor(),
                    tropicalFish.getPatternColor()
            );

            if (!TropicalFish.COMMON_VARIANTS.contains(currentVariant)) {
                cir.setReturnValue(config.glowColor);
            }
        }
    }

    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void alwaysRenderRareFish(double distance, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFish tropicalFish) {
            TropicalFishConfig config = TropicalFishConfig.get();
            if (!config.glowEnabled) {
                return;
            }

            TropicalFish.Variant currentVariant = new TropicalFish.Variant(
                    tropicalFish.getPattern(),
                    tropicalFish.getBaseColor(),
                    tropicalFish.getPatternColor()
            );

            // Keep rare fish rendered (and therefore glowing) as far out as the client
            // knows about them instead of stopping at the size-based render cutoff
            if (!TropicalFish.COMMON_VARIANTS.contains(currentVariant)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void handleSpecialTropicalFishNames(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFish tropicalFish) {
            TropicalFish.Variant currentVariant = new TropicalFish.Variant(
                    tropicalFish.getPattern(),
                    tropicalFish.getBaseColor(),
                    tropicalFish.getPatternColor()
            );

            if (!TropicalFish.COMMON_VARIANTS.contains(currentVariant)) {
                TropicalFishConfig config = TropicalFishConfig.get();

                if (config.namesEnabled) {
                    // Set name if it doesn't have one
                    if (!tropicalFish.hasCustomName()) {
                        String patternName = tropicalFish.getPattern().getSerializedName();
                        String baseColorName = tropicalFish.getBaseColor().getSerializedName().replace('_', ' ');
                        String patternColorName = tropicalFish.getPatternColor().getSerializedName().replace('_', ' ');

                        Component patternText = Component.literal(patternName)
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);

                        Component colorText;
                        if (tropicalFish.getBaseColor() == tropicalFish.getPatternColor()) {
                            colorText = Component.literal("solid " + baseColorName)
                                    .withStyle(ChatFormatting.LIGHT_PURPLE);
                        } else {
                            colorText = Component.literal(baseColorName + " & " + patternColorName)
                                    .withStyle(ChatFormatting.YELLOW);
                        }

                        Component customName = Component.empty()
                                .append(patternText)
                                .append(Component.literal(" ").withStyle(ChatFormatting.RESET))
                                .append(colorText);

                        tropicalFish.setCustomName(customName);
                        tropicalFish.setCustomNameVisible(true);
                    }
                } else {
                    // Remove name if names are disabled
                    if (tropicalFish.hasCustomName()) {
                        tropicalFish.setCustomName(null);
                        tropicalFish.setCustomNameVisible(false);
                    }
                }
            }
        }
    }
}
