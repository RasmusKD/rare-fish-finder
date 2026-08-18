package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.config.TropicalFishConfig.GlowMode;
import com.rasmus.rarefishfinder.util.RareFishVariants;
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
            TropicalFishConfig config = TropicalFishConfig.get();
            GlowMode mode = config.glowMode;
            boolean wanted = mode == GlowMode.ALL
                    || (mode == GlowMode.RARE && RareFishVariants.isRare(tropicalFish));
            if (wanted && config.glowOnlyUncollected
                    && FishCollection.isCollectedFast(FishCollection.packedOf(tropicalFish))) {
                wanted = false;
            }
            if (wanted) {
                cir.setReturnValue(true);
            } else if (mode != GlowMode.OFF && RareFishVariants.isRare(tropicalFish)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void useConfiguredGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFish tropicalFish) {
            TropicalFishConfig config = TropicalFishConfig.get();
            if (config.glowMode == GlowMode.OFF) {
                return;
            }
            if (config.glowOnlyUncollected
                    && FishCollection.isCollectedFast(FishCollection.packedOf(tropicalFish))) {
                return;
            }

            if (RareFishVariants.isRare(tropicalFish)) {
                cir.setReturnValue(config.glowColor);
            } else if (config.glowMode == GlowMode.ALL) {
                // Commons glow muted gray so the rares keep standing out.
                cir.setReturnValue(0xAAAAAA);
            }
        }
    }

    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void alwaysRenderRareFish(double distance, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFish tropicalFish) {
            TropicalFishConfig config = TropicalFishConfig.get();
            if (config.glowMode == GlowMode.OFF) {
                return;
            }
            if (config.glowOnlyUncollected
                    && FishCollection.isCollectedFast(FishCollection.packedOf(tropicalFish))) {
                return;
            }

            // Extended render distance stays rare-only: rendering every
            // common tropical fish at any distance would be pure overhead.
            if (RareFishVariants.isRare(tropicalFish)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void handleSpecialTropicalFishNames(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TropicalFish tropicalFish) {
            FishCollection.markSpotted(FishCollection.packedOf(tropicalFish));
            if (RareFishVariants.isRare(tropicalFish)) {
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
