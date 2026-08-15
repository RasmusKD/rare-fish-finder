package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.util.RareFishVariants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps common tropical fish off Xaero's minimap radar and world map, so the
 * dots that remain are exactly the rare ones. RadarList.add is the single
 * collection point both maps read from.
 *
 * Pseudo: the target only exists when Xaero's Minimap is installed; without
 * it the mixin is skipped and the config option is hidden.
 */
@Pseudo
@Mixin(targets = "xaero.hud.minimap.radar.state.RadarList", remap = false)
public class XaeroRadarListMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void filterRadarEntities(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        TropicalFishConfig config = TropicalFishConfig.get();
        if (config.onlyTropicalFishOnXaeroMap && !(entity instanceof TropicalFish)) {
            cir.setReturnValue(false);
            return;
        }
        if (entity instanceof TropicalFish fish
                && config.hideCommonFishOnXaeroMap
                && !RareFishVariants.isRare(fish)) {
            cir.setReturnValue(false);
        }
    }
}
