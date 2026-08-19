package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ClientSort compat: compareEqualItems is the tie breaker every sort order
 * falls back to when two stacks are the same item, so this one hook groups
 * tropical fish buckets by pattern, then base color, then pattern color in
 * every sort mode. Skipped silently when ClientSort is not installed.
 */
@Mixin(targets = "dev.terminalmc.clientsort.client.order.StackComparison", remap = false)
public class ClientSortComparisonMixin {

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
        int cmp = Integer.compare(rarefishfinder$sortKey(packedA),
                rarefishfinder$sortKey(packedB));
        if (cmp != 0) {
            cir.setReturnValue(cmp);
        }
    }

    private static int rarefishfinder$sortKey(int packed) {
        TropicalFish.Variant variant = new TropicalFish.Variant(packed);
        return (variant.pattern().ordinal() << 16)
                | (variant.baseColor().getId() << 8)
                | variant.patternColor().getId();
    }
}
