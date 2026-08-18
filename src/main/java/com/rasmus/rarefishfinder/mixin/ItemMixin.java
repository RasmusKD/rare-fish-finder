package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltip;
import java.util.Optional;
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
        if (stack.is(Items.TROPICAL_FISH_BUCKET) && TropicalFishConfig.get().showFishInTooltip) {
            cir.setReturnValue(Optional.of(new FishTooltip(stack)));
        }
    }
}
