package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.animal.fish.TropicalFish;
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

    @Inject(method = "extractSlots", at = @At("TAIL"))
    private void drawBucketColors(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            CallbackInfo ci) {
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
            TropicalFish.Variant variant = new TropicalFish.Variant(packed);
            int base = 0xFF000000 | variant.baseColor().getTextureDiffuseColor();
            int pattern = 0xFF000000 | variant.patternColor().getTextureDiffuseColor();
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            extractor.fill(x, y, x + 8, y + 5, 0xE0202020);
            extractor.fill(x + 1, y + 1, x + 4, y + 4, base);
            extractor.fill(x + 4, y + 1, x + 7, y + 4, pattern);
        }
    }
}
