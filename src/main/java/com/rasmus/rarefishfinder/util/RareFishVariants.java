package com.rasmus.rarefishfinder.util;

import net.minecraft.world.entity.animal.fish.TropicalFish;

public final class RareFishVariants {

    private RareFishVariants() {
    }

    /**
     * A fish is rare when its variant is outside the 22 common variants
     * vanilla spawns most of the time. The one definition shared by the glow,
     * name, render-distance and map hooks, so they cannot disagree.
     */
    public static boolean isRare(TropicalFish fish) {
        TropicalFish.Variant variant = new TropicalFish.Variant(
                fish.getPattern(),
                fish.getBaseColor(),
                fish.getPatternColor()
        );
        return !TropicalFish.COMMON_VARIANTS.contains(variant);
    }

    /**
     * Grouping key for sorting buckets: pattern, then base color, then
     * pattern color. Shared by the ClientSort and Mouse Wheelie hooks.
     */
    public static int sortKey(int packed) {
        var variant = new net.minecraft.world.entity.animal.fish.TropicalFish.Variant(packed);
        return (variant.pattern().ordinal() << 8)
                | (variant.baseColor().getId() << 4)
                | variant.patternColor().getId();
    }
}
