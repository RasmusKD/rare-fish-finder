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
        NewCatchToast.textFitted(graphics, font, title, 30, 7, 125, TITLE_COLOR);
        NewCatchToast.textFitted(graphics, font, line, 30, 18, 125, 0xFFFFFFFF);
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
