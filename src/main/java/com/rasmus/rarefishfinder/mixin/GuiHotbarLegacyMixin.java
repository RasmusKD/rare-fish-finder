package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.client.BucketBadge;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1: the HUD class is Gui. Slots record their bucket variants during
 * extraction and the badges flush after the whole hotbar, so they draw on
 * top of the items. The plugin only applies this on 26.1, since 26.2 also
 * has a Gui class with different methods.
 */
@Mixin(targets = "net.minecraft.client.gui.Gui")
public class GuiHotbarLegacyMixin {

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void recordBucketBadge(GuiGraphicsExtractor extractor, int x, int y,
            DeltaTracker deltaTracker, Player player, ItemStack stack, int seed,
            CallbackInfo ci) {
        BucketBadge.record(stack, x, y);
    }

    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    private void drawBucketBadges(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker,
            CallbackInfo ci) {
        BucketBadge.flush(extractor);
    }
}
