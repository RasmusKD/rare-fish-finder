package com.rasmus.rarefishfinder.client;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.ItemStack;

/**
 * The two color squares in the bottom right corner of a tropical fish
 * bucket icon: base color left, pattern color right. Shared by the
 * container screen mixin and the hotbar mixins. The hotbar path records
 * positions during slot extraction and flushes them at the end of the
 * phase, because anything submitted while the items are being extracted
 * lands behind them.
 */
public final class BucketBadge {

    private record Pending(int x, int y, int packed) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private BucketBadge() {
    }

    public static void draw(GuiGraphicsExtractor extractor, int slotX, int slotY, int packed) {
        TropicalFish.Variant variant = new TropicalFish.Variant(packed);
        int base = 0xFF000000 | variant.baseColor().getTextureDiffuseColor();
        int pattern = 0xFF000000 | variant.patternColor().getTextureDiffuseColor();
        // bottom right corner: buckets never stack and have no durability
        // bar, so that spot is always free
        int x = slotX + 8;
        int y = slotY + 11;
        extractor.fill(x, y, x + 8, y + 5, 0xE0202020);
        extractor.fill(x + 1, y + 1, x + 4, y + 4, base);
        extractor.fill(x + 4, y + 1, x + 7, y + 4, pattern);
    }

    /** Hotbar path: remember a slot whose stack carries a variant. */
    public static void record(ItemStack stack, int x, int y) {
        if (!TropicalFishConfig.get().showBucketColors) {
            return;
        }
        int packed = FishTooltipRenderer.bucketVariant(stack);
        if (packed >= 0) {
            PENDING.add(new Pending(x, y, packed));
        }
    }

    /** Hotbar path: draw everything recorded this pass, on top of the items. */
    public static void flush(GuiGraphicsExtractor extractor) {
        for (Pending pending : PENDING) {
            draw(extractor, pending.x(), pending.y(), pending.packed());
        }
        PENDING.clear();
    }
}
