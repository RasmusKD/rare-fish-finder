package com.rasmus.rarefishfinder.gui;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

/**
 * Fancy Toasts compatibility (issue #1): their mixin intercepts addToast and
 * restyles real AdvancementToast instances only, so our own Toast classes
 * pass through with vanilla styling next to their fancy ones. When their mod
 * is present, catches and milestones are announced through a synthetic
 * client-side AdvancementToast instead - same titles, CHALLENGE framing for
 * rares and milestones - which their pipeline picks up like any advancement.
 * The advancement exists only inside the toast: nothing is registered,
 * granted or sent anywhere. Trade-off: the icon is the bucket item, since
 * DisplayInfo icons are item stacks, not renderers; without Fancy Toasts the
 * custom toasts with the live fish model stay exactly as before.
 */
public final class SyntheticAdvancementToasts {

    private static final boolean FANCY_TOASTS_PRESENT =
            FabricLoader.getInstance().isModLoaded("fancytoasts");

    private SyntheticAdvancementToasts() {
    }

    /**
     * A player who installed Fancy Toasts chose its styling, so it wins by
     * default when present; the toggle lets them keep this mod's own fish
     * toast instead.
     */
    public static boolean active() {
        return FANCY_TOASTS_PRESENT
                && com.rasmus.rarefishfinder.config.TropicalFishConfig.get().fancyToastsStyle;
    }

    /**
     * Three tiers of framing: common catches are TASK (the everyday toast),
     * rare catches step up to GOAL, and only milestones get CHALLENGE with
     * its multi-burst fanfare, because a good session catches many fish.
     */
    public static Toast create(Component title, Component line, AdvancementType type, int packed) {
        DisplayInfo display = new DisplayInfo(
                new ItemStackTemplate(Items.TROPICAL_FISH_BUCKET.builtInRegistryHolder(), 1,
                        DataComponentPatch.EMPTY),
                title, line, Optional.empty(),
                type,
                true, false, false);
        Advancement advancement = new Advancement(Optional.empty(), Optional.of(display),
                AdvancementRewards.EMPTY, Map.of(), AdvancementRequirements.allOf(List.of()), false);
        return new AdvancementToast(new AdvancementHolder(
                Identifier.fromNamespaceAndPath("rarefishfinder", "catch_" + packed), advancement));
    }
}
