package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import com.rasmus.rarefishfinder.util.RareFishVariants;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mouse Wheelie compat, the sibling of the ClientSort hook: its
 * compareEqualItems is the tie breaker every middle click sort mode falls
 * back to for equal items, so this groups tropical fish buckets by
 * pattern, then base color, then pattern color. Skipped silently when
 * Mouse Wheelie is not installed.
 */
@Mixin(targets = "de.siphalor.mousewheelie.client.util.ItemStackUtils", remap = false)
public class MouseWheelieComparisonMixin {

    @Inject(method = "compareEqualItems", at = @At("HEAD"), cancellable = true, require = 0)
    private static void groupFishBuckets(ItemStack a, ItemStack b,
            CallbackInfoReturnable<Integer> cir) {
        if (!TropicalFishConfig.get().sortFishBuckets) {
            return;
        }
        int packedA = FishTooltipRenderer.bucketVariant(a);
        if (packedA < 0) {
            return;
        }
        int packedB = FishTooltipRenderer.bucketVariant(b);
        if (packedB < 0) {
            return;
        }
        int cmp = Integer.compare(RareFishVariants.sortKey(packedA),
                RareFishVariants.sortKey(packedB));
        if (cmp != 0) {
            cir.setReturnValue(cmp);
        }
    }
}
