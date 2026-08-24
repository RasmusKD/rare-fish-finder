package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.gui.CollectionScreen;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A cosmetic "Fish Collection" advancement tab, rendered as a real
 * advancement tree inside the vanilla window: tiled background, a root
 * node and one frame per pattern with connecting lines, obtained-frames
 * lighting up as patterns complete - and live fish models as the icons,
 * which the datapack equivalents need a whole resource pack to imitate.
 * Selecting the tab keeps you in the advancements screen; clicking a
 * pattern node opens the collection grid on that pattern, Esc returns.
 * Client-side only: no datapack, no server advancements, no XP.
 *
 * <p>Tab geometry restates the package-private AdvancementTabType.ABOVE
 * constants (28x32, spacing 32, icon offset +6/+9).
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {

    @Shadow
    @Final
    private Map<AdvancementHolder, AdvancementTab> tabs;

    @Unique
    private static final ItemStack RFF_ICON = new ItemStack(Items.TROPICAL_FISH_BUCKET);
    @Unique
    private static final Identifier RFF_TAB_FIRST = Identifier.withDefaultNamespace("advancements/tab_above_left");
    @Unique
    private static final Identifier RFF_TAB_FIRST_SEL = Identifier.withDefaultNamespace("advancements/tab_above_left_selected");
    @Unique
    private static final Identifier RFF_TAB_MIDDLE = Identifier.withDefaultNamespace("advancements/tab_above_middle");
    @Unique
    private static final Identifier RFF_TAB_MIDDLE_SEL = Identifier.withDefaultNamespace("advancements/tab_above_middle_selected");
    @Unique
    private static final Identifier RFF_BACKGROUND = Identifier.withDefaultNamespace("textures/block/prismarine_bricks.png");
    @Unique
    private static final Identifier RFF_FRAME_TASK = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");
    @Unique
    private static final Identifier RFF_FRAME_TASK_DONE = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    @Unique
    private static final Identifier RFF_FRAME_ROOT = Identifier.withDefaultNamespace("advancements/challenge_frame_unobtained");
    @Unique
    private static final Identifier RFF_FRAME_ROOT_DONE = Identifier.withDefaultNamespace("advancements/challenge_frame_obtained");

    @Unique
    private static final TropicalFish.Pattern[] RFF_PATTERNS = TropicalFish.Pattern.values();
    /** Node layout inside the 234x113 interior: root left, 4x3 grid right. */
    @Unique
    private static final int RFF_ROOT_X = 6;
    @Unique
    private static final int RFF_ROOT_Y = 44;
    @Unique
    private static final int RFF_GRID_X = 66;
    @Unique
    private static final int RFF_GRID_Y = 4;
    @Unique
    private static final int RFF_STEP_X = 43;
    @Unique
    private static final int RFF_STEP_Y = 39;

    @Unique
    private boolean rff$selected;

    protected AdvancementsScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private int rff$tabIndex() {
        // ABOVE holds eight tabs; land after the real ones, clamped to the row.
        return Math.min(tabs.size(), 7);
    }

    @Unique
    private int rff$nodeX(int i) {
        return RFF_GRID_X + (i % 4) * RFF_STEP_X;
    }

    @Unique
    private int rff$nodeY(int i) {
        return RFF_GRID_Y + (i / 4) * RFF_STEP_Y;
    }

    @Inject(method = "extractWindow", at = @At("TAIL"))
    private void rff$drawFishTab(GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY,
            CallbackInfo ci) {
        int index = rff$tabIndex();
        int tx = xo + 32 * index;
        int ty = yo - 28;
        Identifier sprite = index == 0
                ? (rff$selected ? RFF_TAB_FIRST_SEL : RFF_TAB_FIRST)
                : (rff$selected ? RFF_TAB_MIDDLE_SEL : RFF_TAB_MIDDLE);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 28, 32);
        graphics.fakeItem(RFF_ICON, tx + 6, ty + 9);
        if (mouseX > tx && mouseX < tx + 28 && mouseY > ty && mouseY < ty + 32) {
            graphics.setTooltipForNextFrame(this.font, Component.literal("Fish Collection"), mouseX, mouseY);
        }

        if (!rff$selected) {
            return;
        }
        rff$drawPage(graphics, xo, yo, mouseX, mouseY);
    }

    @Unique
    private void rff$drawPage(GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY) {
        int ix = xo + 9;
        int iy = yo + 18;

        // Header: cover the underlying tab title with the classic GUI gray
        // the window sprite uses, then our own.
        graphics.fill(xo + 7, yo + 5, xo + 180, yo + 15, 0xFFC6C6C6);
        graphics.text(this.font, Component.literal("Fish Collection").getVisualOrderText(),
                xo + 8, yo + 6, 0xFF404040, false);

        // Tiled background like a real tab page.
        for (int bx = 0; bx < 234; bx += 16) {
            for (int by = 0; by < 113; by += 16) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, RFF_BACKGROUND,
                        ix + bx, iy + by, 0.0F, 0.0F,
                        Math.min(16, 234 - bx), Math.min(16, 113 - by), 16, 16);
            }
        }
        graphics.fill(ix, iy, ix + 234, iy + 113, 0x60000000);

        // Connecting lines, advancement style: root to each row spine.
        int rootCx = ix + RFF_ROOT_X + 13;
        int rootCy = iy + RFF_ROOT_Y + 13;
        int spineX = ix + RFF_GRID_X - 8;
        graphics.fill(rootCx + 13, rootCy - 1, spineX + 1, rootCy + 1, 0xFFFFFFFF);
        int firstRowCy = iy + rff$nodeY(0) + 13;
        int lastRowCy = iy + rff$nodeY(8) + 13;
        graphics.fill(spineX - 1, firstRowCy - 1, spineX + 1, lastRowCy + 1, 0xFFFFFFFF);
        for (int row = 0; row < 3; row++) {
            int cy = iy + rff$nodeY(row * 4) + 13;
            graphics.fill(spineX - 1, cy - 1, ix + rff$nodeX(row * 4) + 1, cy + 1, 0xFFFFFFFF);
        }

        // Root node: challenge frame, obtained when the whole dex is done.
        int total = FishCollection.collectedTotal();
        boolean dexDone = total == FishCollection.TOTAL_VARIANTS;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                dexDone ? RFF_FRAME_ROOT_DONE : RFF_FRAME_ROOT,
                ix + RFF_ROOT_X, iy + RFF_ROOT_Y, 26, 26);
        graphics.fakeItem(RFF_ICON, ix + RFF_ROOT_X + 5, iy + RFF_ROOT_Y + 5);

        // Pattern nodes: task frames with the live poster fish as the icon.
        TropicalFishConfig config = TropicalFishConfig.get();
        int full = 16 * 16;
        for (int i = 0; i < RFF_PATTERNS.length; i++) {
            int nx = ix + rff$nodeX(i);
            int ny = iy + rff$nodeY(i);
            int collected = FishCollection.patternCollected(RFF_PATTERNS[i]);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    collected == full ? RFF_FRAME_TASK_DONE : RFF_FRAME_TASK, nx, ny, 26, 26);
            int poster = FishCollection.firstCollected(RFF_PATTERNS[i]);
            if (poster >= 0) {
                FishTooltipRenderer.extractFish(graphics, poster,
                        nx + 3, ny + 3, nx + 23, ny + 23, 11,
                        config.tooltipFishYaw, config.tooltipFishTilt);
            } else {
                Component unknown = Component.literal("?");
                graphics.text(this.font, unknown.getVisualOrderText(),
                        nx + 13 - this.font.width(unknown) / 2, ny + 9, 0xFFAAAAAA, true);
            }
        }

        // Footer progress on the page itself.
        graphics.text(this.font, Component.literal("Total: " + total + "/" + FishCollection.TOTAL_VARIANTS)
                        .getVisualOrderText(), ix + 4, iy + 113 - 11, 0xFFFFFFFF, true);

        // Advancement-style hover tooltips, after everything else.
        if (mouseX >= ix + RFF_ROOT_X && mouseX < ix + RFF_ROOT_X + 26
                && mouseY >= iy + RFF_ROOT_Y && mouseY < iy + RFF_ROOT_Y + 26) {
            graphics.setTooltipForNextFrame(this.font, Component.literal(
                    "Fish Collection - " + total + "/" + FishCollection.TOTAL_VARIANTS), mouseX, mouseY);
        }
        for (int i = 0; i < RFF_PATTERNS.length; i++) {
            int nx = ix + rff$nodeX(i);
            int ny = iy + rff$nodeY(i);
            if (mouseX >= nx && mouseX < nx + 26 && mouseY >= ny && mouseY < ny + 26) {
                int collected = FishCollection.patternCollected(RFF_PATTERNS[i]);
                graphics.setTooltipForNextFrame(this.font, Component.literal(
                        rff$nice(RFF_PATTERNS[i].getSerializedName()) + " - " + collected + "/" + full),
                        mouseX, mouseY);
            }
        }
    }

    @Unique
    private static String rff$nice(String serialized) {
        String spaced = serialized.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rff$clickFishTab(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) {
            return;
        }
        int xo = (this.width - 252) / 2;
        int yo = (this.height - 140) / 2;
        int tx = xo + 32 * rff$tabIndex();
        int ty = yo - 28;
        if (event.x() > tx && event.x() < tx + 28 && event.y() > ty && event.y() < ty + 32) {
            rff$selected = true;
            cir.setReturnValue(true);
            return;
        }
        if (!rff$selected) {
            return;
        }
        // Our page is open: a click on a pattern node drills into the grid,
        // a click on a real tab hands the screen back to vanilla.
        int ix = xo + 9;
        int iy = yo + 18;
        for (int i = 0; i < RFF_PATTERNS.length; i++) {
            int nx = ix + rff$nodeX(i);
            int ny = iy + rff$nodeY(i);
            if (event.x() >= nx && event.x() < nx + 26 && event.y() >= ny && event.y() < ny + 26) {
                this.minecraft.setScreenAndShow(new CollectionScreen((Screen) (Object) this, i));
                cir.setReturnValue(true);
                return;
            }
        }
        for (AdvancementTab tab : tabs.values()) {
            if (tab.isMouseOver(xo, yo, event.x(), event.y())) {
                rff$selected = false;
                return; // vanilla handles the actual selection
            }
        }
        // Swallow clicks inside the window while our page is up, so the
        // underlying vanilla tab's widgets are not clicked through it.
        if (event.x() >= ix && event.x() < ix + 234 && event.y() >= iy && event.y() < iy + 113) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractInside", at = @At("HEAD"), cancellable = true)
    private void rff$suppressInside(GuiGraphicsExtractor graphics, int xo, int yo, CallbackInfo ci) {
        if (rff$selected) {
            // Our page replaces the tab contents; the frame and our drawing
            // happen in extractWindow afterwards.
            graphics.fill(xo + 9, yo + 18, xo + 9 + 234, yo + 18 + 113, 0xFF000000);
            ci.cancel();
        }
    }

    @Inject(method = "extractTooltips", at = @At("HEAD"), cancellable = true)
    private void rff$suppressTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int xo, int yo,
            CallbackInfo ci) {
        if (rff$selected) {
            ci.cancel();
        }
    }
}
