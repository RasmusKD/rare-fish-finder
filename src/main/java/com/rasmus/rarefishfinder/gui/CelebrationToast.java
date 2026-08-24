package com.rasmus.rarefishfinder.gui;

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
import org.jspecify.annotations.Nullable;

/**
 * The big moments: a pattern page filled, all commons, all solids, or the
 * whole dex. Gold title, the challenge fanfare, and the fish that closed
 * the milestone as the icon. Shown instead of the plain new-catch toast,
 * and a little longer.
 */
public final class CelebrationToast implements Toast {

    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
    private static final double DISPLAY_MS = 8000.0;
    private static final int TITLE_COLOR = 0xFFFF8930;

    private final int packed;
    private final Component title;
    private final Component line;
    private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;

    public CelebrationToast(Component title, Component line, int packed) {
        this.title = title;
        this.line = line;
        this.packed = packed;
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
        return SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width(), height());
        graphics.text(font, font.split(title, 125).get(0), 30, 7, TITLE_COLOR, false);
        graphics.text(font, font.split(line, 125).get(0), 30, 18, 0xFFFFFFFF, false);
        TropicalFishConfig config = TropicalFishConfig.get();
        // The entity submit takes absolute screen coordinates and ignores the
        // pose (unlike text and sprites), while the toast manager positions
        // this toast via a pose translation - so the box must be offset by
        // the pose's current translation or the fish renders clipped in the
        // screen corner instead of in the toast.
        var pose = graphics.pose();
        int ox = Math.round(pose.m20);
        int oy = Math.round(pose.m21);
        // Size follows the hover-preview ratio (scale ~0.9x the box height);
        // 12 in a 24px box rendered the fish tiny.
        FishTooltipRenderer.extractFish(graphics, packed, ox + 2, oy + 2, ox + 30, oy + 30, 24,
                config.tooltipFishYaw, config.tooltipFishTilt);
    }
}
