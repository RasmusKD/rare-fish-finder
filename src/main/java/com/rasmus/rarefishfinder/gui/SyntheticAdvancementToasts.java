package com.rasmus.rarefishfinder.gui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
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
import org.jspecify.annotations.Nullable;

/**
 * Fancy Toasts compatibility (issue #1): their mixin intercepts addToast and
 * restyles real AdvancementToast instances only, so our own Toast classes
 * pass through with vanilla styling next to their fancy ones. When their mod
 * is present, catches and milestones are announced through a synthetic
 * client-side AdvancementToast instead, with the same titles and CHALLENGE
 * framing for rares and milestones, which their pipeline picks up like any
 * advancement.
 * The advancement exists only inside the toast: nothing is registered,
 * granted or sent anywhere. FancyToastBridgeMixin then draws the live fish
 * instead of the bucket icon and puts our banner line in place of their type
 * name.
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
     * What Fancy Toasts needs to show one of our toasts the way we mean it:
     * the fish to draw as the icon, and for catches the banner line ("Rare
     * fish collected!") that replaces its advancement type name.
     *
     * @param banner null keeps their type name (milestones, whose title says it)
     */
    public record Tag(int packed, @Nullable Component banner) {
    }

    // Keyed by our title component: Fancy Toasts copies it into its own
    // display object unchanged, so the bridge mixin can look a toast up from
    // what it holds. Weak, so a toast that is gone leaves nothing behind.
    private static final Map<Component, Tag> TAGS = Collections.synchronizedMap(new WeakHashMap<>());

    /** The tag of one of our synthetic toasts, or null when the title is not ours. */
    public static @Nullable Tag tagFor(@Nullable Component title) {
        return title == null ? null : TAGS.get(title);
    }

    /**
     * Their banner line is the advancement type's name ("Goal Reached!"),
     * never ours, so a catch would lose whether it was rare. The banner is
     * kept in the tag and swapped in by the bridge mixin. Framing follows the
     * plain popup: rare catches and milestones are CHALLENGE (their fanfare
     * sound), common catches TASK.
     */
    public static Toast create(Component title, @Nullable Component banner, Component description,
            AdvancementType type, int packed) {
        DisplayInfo display = new DisplayInfo(
                new ItemStackTemplate(Items.TROPICAL_FISH_BUCKET.builtInRegistryHolder(), 1,
                        DataComponentPatch.EMPTY),
                title, description, Optional.empty(),
                type,
                true, false, false);
        TAGS.put(title, new Tag(packed, banner));
        Advancement advancement = new Advancement(Optional.empty(), Optional.of(display),
                AdvancementRewards.EMPTY, Map.of(), AdvancementRequirements.allOf(List.of()), false);
        return new AdvancementToast(new AdvancementHolder(
                Identifier.fromNamespaceAndPath("rarefishfinder", "catch_" + packed), advancement));
    }
}
