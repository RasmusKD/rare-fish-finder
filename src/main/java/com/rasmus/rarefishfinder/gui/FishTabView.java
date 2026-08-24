package com.rasmus.rarefishfinder.gui;

import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
 * <p>Every drawing entry point takes the window origin and mouse position
 * explicitly rather than reading them off the screen. AdvancementsScreen
 * passes different subsets of those four numbers on 26.1 and 26.2 (see
 * VERSIONS.md), so the mixin pair resolves them per version and this class
 * never has to know which version it is running on.
 *
 * <p>Tab geometry restates the package-private AdvancementTabType.ABOVE
 * constants (28x32, spacing 32, icon offset +6/+9).
 */
public final class FishTabView {

    /** Vanilla advancement window size; the origin derives from it. */
    public static final int WINDOW_WIDTH = 252;
    public static final int WINDOW_HEIGHT = 140;

    private static final ItemStack ICON = new ItemStack(Items.TROPICAL_FISH_BUCKET);
    private static final Identifier TAB_FIRST = Identifier.withDefaultNamespace("advancements/tab_above_left");
    private static final Identifier TAB_FIRST_SEL = Identifier.withDefaultNamespace("advancements/tab_above_left_selected");
    private static final Identifier TAB_MIDDLE = Identifier.withDefaultNamespace("advancements/tab_above_middle");
    private static final Identifier TAB_MIDDLE_SEL = Identifier.withDefaultNamespace("advancements/tab_above_middle_selected");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/block/prismarine_bricks.png");
    private static final Identifier FRAME_TASK = Identifier.withDefaultNamespace("advancements/task_frame_unobtained");
    private static final Identifier FRAME_TASK_DONE = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    private static final Identifier FRAME_ROOT = Identifier.withDefaultNamespace("advancements/challenge_frame_unobtained");
    private static final Identifier FRAME_ROOT_DONE = Identifier.withDefaultNamespace("advancements/challenge_frame_obtained");

    private static final TropicalFish.Pattern[] PATTERNS = TropicalFish.Pattern.values();
    /** Node layout inside the 234x113 interior: root left, 4x3 grid right. */
    private static final int ROOT_X = 6;
    private static final int ROOT_Y = 44;
    private static final int GRID_X = 66;
    private static final int GRID_Y = 4;
    private static final int STEP_X = 43;
    private static final int STEP_Y = 39;

    private boolean selected;

    /** Window origin, the way vanilla 26.1 computed it before passing it down. */
    public static int originX(int screenWidth) {
        return (screenWidth - WINDOW_WIDTH) / 2;
    }

    public static int originY(int screenHeight) {
        return (screenHeight - WINDOW_HEIGHT) / 2;
    }

    public boolean selected() {
        return selected;
    }

    private int tabIndex(Map<AdvancementHolder, AdvancementTab> tabs) {
        // ABOVE holds eight tabs; land after the real ones, clamped to the row.
        return Math.min(tabs.size(), 7);
    }

    private static int nodeX(int i) {
        return GRID_X + (i % 4) * STEP_X;
    }

    private static int nodeY(int i) {
        return GRID_Y + (i / 4) * STEP_Y;
    }

    /** The tab button, plus our page when it is the selected one. */
    public void drawTab(Font font, Map<AdvancementHolder, AdvancementTab> tabs,
            GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY) {
        int index = tabIndex(tabs);
        int tx = xo + 32 * index;
        int ty = yo - 28;
        Identifier sprite = index == 0
                ? (selected ? TAB_FIRST_SEL : TAB_FIRST)
                : (selected ? TAB_MIDDLE_SEL : TAB_MIDDLE);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tx, ty, 28, 32);
        graphics.fakeItem(ICON, tx + 6, ty + 9);
        if (mouseX > tx && mouseX < tx + 28 && mouseY > ty && mouseY < ty + 32) {
            graphics.setTooltipForNextFrame(font, Component.literal("Fish Collection"), mouseX, mouseY);
        }

        if (!selected) {
            return;
        }
        drawPage(font, graphics, xo, yo, mouseX, mouseY);
    }

    private void drawPage(Font font, GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY) {
        int ix = xo + 9;
        int iy = yo + 18;

        // Header: cover the underlying tab title with the classic GUI gray
        // the window sprite uses, then our own.
        graphics.fill(xo + 7, yo + 5, xo + 180, yo + 15, 0xFFC6C6C6);
        graphics.text(font, Component.literal("Fish Collection").getVisualOrderText(),
                xo + 8, yo + 6, 0xFF404040, false);

        // Tiled background like a real tab page.
        for (int bx = 0; bx < 234; bx += 16) {
            for (int by = 0; by < 113; by += 16) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
                        ix + bx, iy + by, 0.0F, 0.0F,
                        Math.min(16, 234 - bx), Math.min(16, 113 - by), 16, 16);
            }
        }
        graphics.fill(ix, iy, ix + 234, iy + 113, 0x60000000);

        // Connecting lines, advancement style: root to each row spine.
        int rootCx = ix + ROOT_X + 13;
        int rootCy = iy + ROOT_Y + 13;
        int spineX = ix + GRID_X - 8;
        graphics.fill(rootCx + 13, rootCy - 1, spineX + 1, rootCy + 1, 0xFFFFFFFF);
        int firstRowCy = iy + nodeY(0) + 13;
        int lastRowCy = iy + nodeY(8) + 13;
        graphics.fill(spineX - 1, firstRowCy - 1, spineX + 1, lastRowCy + 1, 0xFFFFFFFF);
        for (int row = 0; row < 3; row++) {
            int cy = iy + nodeY(row * 4) + 13;
            graphics.fill(spineX - 1, cy - 1, ix + nodeX(row * 4) + 1, cy + 1, 0xFFFFFFFF);
        }

        // Root node: challenge frame, obtained when the whole dex is done.
        int total = FishCollection.collectedTotal();
        boolean dexDone = total == FishCollection.TOTAL_VARIANTS;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                dexDone ? FRAME_ROOT_DONE : FRAME_ROOT,
                ix + ROOT_X, iy + ROOT_Y, 26, 26);
        graphics.fakeItem(ICON, ix + ROOT_X + 5, iy + ROOT_Y + 5);

        // Pattern nodes: task frames with the live poster fish as the icon.
        TropicalFishConfig config = TropicalFishConfig.get();
        int full = 16 * 16;
        for (int i = 0; i < PATTERNS.length; i++) {
            int nx = ix + nodeX(i);
            int ny = iy + nodeY(i);
            int collected = FishCollection.patternCollected(PATTERNS[i]);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    collected == full ? FRAME_TASK_DONE : FRAME_TASK, nx, ny, 26, 26);
            int poster = FishCollection.firstCollected(PATTERNS[i]);
            if (poster >= 0) {
                FishTooltipRenderer.extractFish(graphics, poster,
                        nx + 3, ny + 3, nx + 23, ny + 23, 11,
                        config.tooltipFishYaw, config.tooltipFishTilt);
            } else {
                Component unknown = Component.literal("?");
                graphics.text(font, unknown.getVisualOrderText(),
                        nx + 13 - font.width(unknown) / 2, ny + 9, 0xFFAAAAAA, true);
            }
        }

        // Footer progress on the page itself.
        graphics.text(font, Component.literal("Total: " + total + "/" + FishCollection.TOTAL_VARIANTS)
                .getVisualOrderText(), ix + 4, iy + 113 - 11, 0xFFFFFFFF, true);

        // Advancement-style hover tooltips, after everything else.
        if (mouseX >= ix + ROOT_X && mouseX < ix + ROOT_X + 26
                && mouseY >= iy + ROOT_Y && mouseY < iy + ROOT_Y + 26) {
            graphics.setTooltipForNextFrame(font, Component.literal(
                    "Fish Collection - " + total + "/" + FishCollection.TOTAL_VARIANTS), mouseX, mouseY);
        }
        for (int i = 0; i < PATTERNS.length; i++) {
            int nx = ix + nodeX(i);
            int ny = iy + nodeY(i);
            if (mouseX >= nx && mouseX < nx + 26 && mouseY >= ny && mouseY < ny + 26) {
                int collected = FishCollection.patternCollected(PATTERNS[i]);
                graphics.setTooltipForNextFrame(font, Component.literal(
                        nice(PATTERNS[i].getSerializedName()) + " - " + collected + "/" + full),
                        mouseX, mouseY);
            }
        }
    }

    private static String nice(String serialized) {
        String spaced = serialized.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /** Our page replaces the tab contents while it is selected. */
    public boolean suppressInside(GuiGraphicsExtractor graphics, int xo, int yo) {
        if (!selected) {
            return false;
        }
        // The frame and our drawing happen in extractWindow afterwards.
        graphics.fill(xo + 9, yo + 18, xo + 9 + 234, yo + 18 + 113, 0xFF000000);
        return true;
    }

    public boolean suppressTooltips() {
        return selected;
    }

    /** @return true when the click is ours and vanilla must not see it. */
    public boolean click(Screen parent, Minecraft minecraft, Map<AdvancementHolder, AdvancementTab> tabs,
            int screenWidth, int screenHeight, MouseButtonEvent event) {
        if (event.button() != 0) {
            return false;
        }
        int xo = originX(screenWidth);
        int yo = originY(screenHeight);
        int tx = xo + 32 * tabIndex(tabs);
        int ty = yo - 28;
        if (event.x() > tx && event.x() < tx + 28 && event.y() > ty && event.y() < ty + 32) {
            selected = true;
            return true;
        }
        if (!selected) {
            return false;
        }
        // Our page is open: a click on a pattern node drills into the grid,
        // a click on a real tab hands the screen back to vanilla.
        int ix = xo + 9;
        int iy = yo + 18;
        for (int i = 0; i < PATTERNS.length; i++) {
            int nx = ix + nodeX(i);
            int ny = iy + nodeY(i);
            if (event.x() >= nx && event.x() < nx + 26 && event.y() >= ny && event.y() < ny + 26) {
                minecraft.setScreenAndShow(new CollectionScreen(parent, i));
                return true;
            }
        }
        for (AdvancementTab tab : tabs.values()) {
            if (tab.isMouseOver(xo, yo, event.x(), event.y())) {
                selected = false;
                return false; // vanilla handles the actual selection
            }
        }
        // Swallow clicks inside the window while our page is up, so the
        // underlying vanilla tab's widgets are not clicked through it.
        return event.x() >= ix && event.x() < ix + 234 && event.y() >= iy && event.y() < iy + 113;
    }
}
