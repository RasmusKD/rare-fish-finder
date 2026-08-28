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
        this.title = titleFor(rare);
        this.line = lineFor(packed);
    }

    /** Shared with the Fancy Toasts bridge, so both presentations say the same thing. */
    public static Component titleFor(boolean rare) {
        return Component.translatable(rare
                ? "toast.rarefishfinder.new_rare"
                : "toast.rarefishfinder.new_fish");
    }

    public static Component lineFor(int packed) {
        return Component.literal("#" + FishCollection.numberOf(packed) + " ")
                .append(variantName(packed));
    }

    /**
     * Advancement toasts (vanilla and Fancy Toasts) display only the
     * advancement's title, never its description - so for the synthetic
     * catch toasts the number line IS the title. The pass-through
     * translatable keeps the "toast.rarefishfinder." key that the Fancy
     * Toasts icon mixin recognises our toasts by.
     */
    public static Component numberedTitleFor(int packed) {
        return Component.translatable("toast.rarefishfinder.entry", lineFor(packed));
    }

    private static Component variantName(int packed) {
        Integer commonIndex = commonIndexOf(packed);
        if (commonIndex != null) {
            return Component.translatable("entity.minecraft.tropical_fish.predefined." + commonIndex);
        }
        // Vanilla's own pattern and color keys, so the line localizes with
        // the game ("Hvid / Limegrøn" on a Danish client, not "White / Lime").
        TropicalFish.Variant variant = new TropicalFish.Variant(packed);
        Component type = Component.translatable(
                "entity.minecraft.tropical_fish.type." + variant.pattern().getSerializedName());
        Component colors = variant.baseColor() == variant.patternColor()
                ? Component.translatable("color.minecraft." + variant.baseColor().getName())
                : Component.empty()
                        .append(Component.translatable("color.minecraft." + variant.baseColor().getName()))
                        .append(" / ")
                        .append(Component.translatable("color.minecraft." + variant.patternColor().getName()));
        return Component.empty().append(type).append(" ").append(colors);
    }

    private static @Nullable Integer commonIndexOf(int packed) {
        for (int i = 0; i < TropicalFish.COMMON_VARIANTS.size(); i++) {
            if (TropicalFish.COMMON_VARIANTS.get(i).getPackedId() == packed) {
                return i;
            }
        }
        return null;
    }


    /**
     * One line, whole meaning: instead of vanilla's split-and-truncate, a
     * line wider than the space shrinks just enough to fit, so long variant
     * names ("#2152 Glitter Light Blue / Purple") stay readable in full.
     * Shared with CelebrationToast.
     */
    static void textFitted(GuiGraphicsExtractor graphics, Font font, Component text,
            int x, int y, int maxWidth, int color) {
        var order = text.getVisualOrderText();
        int width = font.width(order);
        if (width <= maxWidth) {
            graphics.text(font, order, x, y, color, false);
            return;
        }
        float scale = maxWidth / (float) width;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y + (9.0F - 9.0F * scale) / 2.0F);
        pose.scale(scale, scale);
        graphics.text(font, order, 0, 0, color, false);
        pose.popMatrix();
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
        // The challenge fanfare is reserved for milestones: a good session
        // catches many rares, and the gold title carries the specialness.
        return null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width(), height());
        textFitted(graphics, font, title, 30, 7, 125,
                rare ? RARE_TITLE_COLOR : COMMON_TITLE_COLOR);
        textFitted(graphics, font, line, 30, 18, 125, 0xFFFFFFFF);
        TropicalFishConfig config = TropicalFishConfig.get();
        // The entity submit takes absolute screen coordinates and ignores the
        // pose (unlike text and sprites), while the toast manager positions
        // this toast via a pose translation - so the box must be offset by
        // the pose's current translation or the fish renders clipped in the
        // screen corner instead of in the toast.
        var pose = graphics.pose();
        int ox = Math.round(pose.m20);
        int oy = Math.round(pose.m21);
        // Vanilla toast size; the fish shrinks to fit its 28px corner, same
        // fixed pose as the tooltips and the collection screen.
        FishTooltipRenderer.extractFish(graphics, packed, ox + 2, oy + 2, ox + 30, oy + 30, 26,
                config.tooltipFishYaw, config.tooltipFishTilt);
    }
}
