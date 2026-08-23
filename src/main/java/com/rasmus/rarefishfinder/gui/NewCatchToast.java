package com.rasmus.rarefishfinder.gui;

import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import org.jspecify.annotations.Nullable;

/**
 * Advancement-style popup for the first catch of a variant (issue #1): the
 * actual fish as the icon (same renderer as tooltips and the collection
 * screen), a gold title and the challenge fanfare for rares, and the
 * variant's collection number so progress shows without opening the screen.
 */
public class NewCatchToast implements Toast {

    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
    private static final double DISPLAY_MS = 5000.0;
    private static final int RARE_TITLE_COLOR = 0xFFFF8930;
    private static final int COMMON_TITLE_COLOR = 0xFFFFFF00;

    private final int packed;
    private final boolean rare;
    private final Component title;
    private final Component line;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public NewCatchToast(int packed, boolean rare) {
        this.packed = packed;
        this.rare = rare;
        this.title = Component.translatable(rare
                ? "toast.rarefishfinder.new_rare"
                : "toast.rarefishfinder.new_fish");
        this.line = Component.literal("#" + FishCollection.numberOf(packed) + " ")
                .append(variantName(packed));
    }

    private static Component variantName(int packed) {
        Integer commonIndex = commonIndexOf(packed);
        if (commonIndex != null) {
            return Component.translatable("entity.minecraft.tropical_fish.predefined." + commonIndex);
        }
        TropicalFish.Variant variant = new TropicalFish.Variant(packed);
        return Component.literal(nice(variant.baseColor().getName()) + " / "
                + nice(variant.patternColor().getName()));
    }

    private static @Nullable Integer commonIndexOf(int packed) {
        for (int i = 0; i < TropicalFish.COMMON_VARIANTS.size(); i++) {
            if (TropicalFish.COMMON_VARIANTS.get(i).getPackedId() == packed) {
                return i;
            }
        }
        return null;
    }

    public static String nice(String colorName) {
        String spaced = colorName.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long fullyVisibleForMs) {
        wantedVisibility = fullyVisibleForMs >= DISPLAY_MS * manager.getNotificationDisplayTimeMultiplier()
                ? Toast.Visibility.HIDE
                : Toast.Visibility.SHOW;
    }

    @Override
    public @Nullable SoundEvent getSoundEvent() {
        return rare ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width(), height());
        graphics.text(font, font.split(title, 125).get(0), 30, 7,
                rare ? RARE_TITLE_COLOR : COMMON_TITLE_COLOR, false);
        graphics.text(font, font.split(line, 125).get(0), 30, 18, 0xFFFFFFFF, false);
        TropicalFishConfig config = TropicalFishConfig.get();
        FishTooltipRenderer.extractFish(graphics, packed, 4, 4, 28, 28, 12,
                config.tooltipFishYaw, config.tooltipFishTilt);
    }
}
