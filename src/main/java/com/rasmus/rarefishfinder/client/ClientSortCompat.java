package com.rasmus.rarefishfinder.client;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Registers two extra ClientSort orders when the mod is installed:
 * "fishPattern" groups tropical fish buckets by pattern first, "fishColor"
 * by base color first. Bind them to different modifiers in ClientSort's
 * config (Default/Shift/Ctrl/Alt Sort Order). The ClientSort classes are
 * only referenced from the inner class, so nothing here loads when the mod
 * is absent.
 */
public final class ClientSortCompat {

    private ClientSortCompat() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("clientsort")) {
            return;
        }
        try {
            Orders.register();
        } catch (Throwable t) {
            RareFishFinderClient.LOGGER.warn("ClientSort compat failed to register", t);
        }
    }

    private static final class Orders {

        static void register() {
            dev.terminalmc.clientsort.client.order.SortOrder.register(new FishOrder("fishPattern", true));
            dev.terminalmc.clientsort.client.order.SortOrder.register(new FishOrder("fishColor", false));
            RareFishFinderClient.LOGGER.info("Registered ClientSort orders fishPattern and fishColor");
        }
    }

    private static final class FishOrder extends dev.terminalmc.clientsort.client.order.SortOrder {

        private final boolean patternFirst;

        FishOrder(String name, boolean patternFirst) {
            super(name);
            this.patternFirst = patternFirst;
        }

        /**
         * Alphabetical like the stock order, but ties between fish buckets
         * are broken by the chosen variant key before ClientSort's own
         * equal-item comparison gets a say.
         */
        @Override
        public int[] sort(int[] ids, net.minecraft.world.item.ItemStack[] stacks,
                dev.terminalmc.clientsort.client.order.SortContext context) {
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
                return dev.terminalmc.clientsort.client.order.StackComparison
                        .compareEqualItems(stacks[a], stacks[b], context);
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
