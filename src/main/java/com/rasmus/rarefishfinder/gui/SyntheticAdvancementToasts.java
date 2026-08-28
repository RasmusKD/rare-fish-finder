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

    /** True when announcements should go through a real AdvancementToast. */
    public static boolean active() {
        return FANCY_TOASTS_PRESENT;
    }

    public static Toast create(Component title, Component line, boolean challenge, int packed) {
        DisplayInfo display = new DisplayInfo(
                new ItemStackTemplate(Items.TROPICAL_FISH_BUCKET.builtInRegistryHolder(), 1,
                        DataComponentPatch.EMPTY),
                title, line, Optional.empty(),
                challenge ? AdvancementType.CHALLENGE : AdvancementType.TASK,
                true, false, false);
        Advancement advancement = new Advancement(Optional.empty(), Optional.of(display),
                AdvancementRewards.EMPTY, Map.of(), AdvancementRequirements.allOf(List.of()), false);
        return new AdvancementToast(new AdvancementHolder(
                Identifier.fromNamespaceAndPath("rarefishfinder", "catch_" + packed), advancement));
    }
}
