package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.client.BucketBadge;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two small color squares in the top left corner of every tropical fish
 * bucket icon, base color left and pattern color right, so a chest full of
 * buckets can be told apart without hovering each one.
 */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    // extractContents TAIL, not extractSlots: the slot items land on a later
    // batch, so anything submitted inside extractSlots draws behind them.
    // extractContents ends after the nextStratum call (above every item) but
    // before tooltips, the exact phase ItemLocks draws its lock icons in.
    @Inject(method = "extractContents", at = @At("TAIL"))
    private void drawBucketColors(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        if (!TropicalFishConfig.get().showBucketColors) {
            return;
        }
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        for (Slot slot : self.getMenu().slots) {
            if (!slot.isActive()) {
                continue;
            }
            int packed = FishTooltipRenderer.bucketVariant(slot.getItem());
            if (packed < 0) {
                continue;
            }
            BucketBadge.draw(extractor, leftPos + slot.x, topPos + slot.y, packed);
        }
    }
}
