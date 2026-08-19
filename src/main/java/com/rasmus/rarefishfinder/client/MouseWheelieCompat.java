package com.rasmus.rarefishfinder.client;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Registers the two fish sort modes with Mouse Wheelie when it is
 * installed, the sibling of ClientSortCompat: "fishPattern" groups
 * tropical fish buckets by pattern first, "fishColor" by base color
 * first. Assign them to the primary, shift or control slot in Mouse
 * Wheelie's config. The Mouse Wheelie classes are only referenced from
 * the inner classes, so nothing here loads when the mod is absent.
 */
public final class MouseWheelieCompat {

    private MouseWheelieCompat() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("mousewheelie")) {
            return;
        }
        try {
            Modes.register();
        } catch (Throwable t) {
            RareFishFinderClient.LOGGER.warn("Mouse Wheelie compat failed to register", t);
        }
    }

    private static final class Modes {

        static void register() {
            de.siphalor.mousewheelie.client.inventory.sort.SortMode.register(new FishMode("fishPattern", true));
            de.siphalor.mousewheelie.client.inventory.sort.SortMode.register(new FishMode("fishColor", false));
            RareFishFinderClient.LOGGER.info("Registered Mouse Wheelie sort modes fishPattern and fishColor");
        }
    }

    private static final class FishMode extends de.siphalor.mousewheelie.client.inventory.sort.SortMode {

        private final boolean patternFirst;

        FishMode(String name, boolean patternFirst) {
            super(name);
            this.patternFirst = patternFirst;
        }

        /**
         * Alphabetical like the stock mode, but ties between fish buckets
         * are broken by the chosen variant key before Mouse Wheelie's own
         * equal-item comparison gets a say.
         */
        @Override
        public int[] sort(int[] ids, net.minecraft.world.item.ItemStack[] stacks,
                de.siphalor.mousewheelie.client.inventory.sort.SortContext context) {
            String[] names = new String[stacks.length];
            int[] fishKeys = new int[stacks.length];
            for (int i = 0; i < stacks.length; i++) {
                net.minecraft.world.item.ItemStack stack = stacks[i];
                names[i] = stack.isEmpty() ? null : stack.getHoverName().getString();
                int packed = com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer.bucketVariant(stack);
                fishKeys[i] = packed < 0 ? -1 : variantKey(packed);
            }

            Integer[] order = new Integer[ids.length];
            for (int i = 0; i < ids.length; i++) {
                order[i] = ids[i];
            }
            java.util.Arrays.sort(order, (a, b) -> {
                String nameA = names[a];
                String nameB = names[b];
                if (nameA == null) {
                    return nameB == null ? 0 : 1;
                }
                if (nameB == null) {
                    return -1;
                }
                int cmp = nameA.compareToIgnoreCase(nameB);
                if (cmp != 0) {
                    return cmp;
                }
                if (fishKeys[a] >= 0 && fishKeys[b] >= 0) {
                    cmp = Integer.compare(fishKeys[a], fishKeys[b]);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                return de.siphalor.mousewheelie.client.util.ItemStackUtils
                        .compareEqualItems(stacks[a], stacks[b]);
            });

            int[] out = new int[ids.length];
            for (int i = 0; i < out.length; i++) {
                out[i] = order[i];
            }
            return out;
        }

        private int variantKey(int packed) {
            var variant = new net.minecraft.world.entity.animal.fish.TropicalFish.Variant(packed);
            int pattern = variant.pattern().ordinal();
            int base = variant.baseColor().getId();
            int patternColor = variant.patternColor().getId();
            return patternFirst
                    ? (pattern << 8) | (base << 4) | patternColor
                    : (base << 8) | (patternColor << 4) | pattern;
        }
    }
}
