package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltip;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "getTooltipImage(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = true)
    private void showFishModelInBucketTooltip(ItemStack stack,
            CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (stack.is(Items.TROPICAL_FISH_BUCKET) && TropicalFishConfig.get().showFishInTooltip
                && hasVariantData(stack)) {
            cir.setReturnValue(Optional.of(new FishTooltip(stack)));
        }
    }

    /**
     * The generic bucket in the creative inventory and recipe viewers has no
     * variant data. Showing it as the default variant would be a lie, and
     * with hover-collect on it would mark a fish nobody caught as collected.
     */
    private static boolean hasVariantData(ItemStack stack) {
        if (stack.has(DataComponents.TROPICAL_FISH_PATTERN)) {
            return true;
        }
        var data = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        return data != null && data.copyTag().getIntOr("BucketVariantTag", -1) >= 0;
    }
}
