package com.rasmus.rarefishfinder.gui;

import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.DyeColor;

/**
 * The fish collection ("fishdex"): one tab per pattern, each a 15x15 grid of
 * base color x pattern color. Cell states: dark = never seen, dim colors =
 * spotted swimming, full colors = collected in a bucket.
 */
public final class CollectionScreen extends Screen {
    private static final TropicalFish.Pattern[] PATTERNS = TropicalFish.Pattern.values();
    private static final DyeColor[] COLORS = DyeColor.values();
    private static final int CELL = 12;
    private static final int GRID = CELL * COLORS.length;
    private static final int PANEL_PAD = 10;
    private static final int INFO_WIDTH = 130;

    // Packed variant id -> index into COMMON_VARIANTS, which is also the
    // index of the official vanilla name (predefined.0 = Anemone, ...).
    private static final Map<Integer, Integer> COMMON_INDEX = buildCommonIndex();

    private static Map<Integer, Integer> buildCommonIndex() {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < TropicalFish.COMMON_VARIANTS.size(); i++) {
            map.put(TropicalFish.COMMON_VARIANTS.get(i).getPackedId(), i);
        }
        return map;
    }

    /** -1 = the overview front page; 0..11 = one pattern's color grid. */
    private static final int OVERVIEW = -1;
    private int patternIndex = OVERVIEW;
    private int panelX;
    private int panelY;
    private int gridX;
    private int gridY;

    public CollectionScreen() {
        super(Component.literal("Fish Collection"));
    }

    @Override
    protected void init() {
        int panelWidth = PANEL_PAD + GRID + PANEL_PAD + INFO_WIDTH + PANEL_PAD;
        int panelHeight = PANEL_PAD + 24 + GRID + PANEL_PAD;
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        gridX = panelX + PANEL_PAD;
        gridY = panelY + PANEL_PAD + 24;

        addRenderableWidget(Button.builder(Component.literal("<"), b -> switchPattern(-1))
                .bounds(gridX, panelY + PANEL_PAD, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> switchPattern(1))
                .bounds(gridX + GRID - 20, panelY + PANEL_PAD, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(panelX + panelWidth - PANEL_PAD - 60,
                        gridY + GRID - 16, 60, 16).build());
    }

    private void switchPattern(int direction) {
        // 13 pages: the overview, then the 12 patterns.
        patternIndex = Math.floorMod(patternIndex + 1 + direction, PATTERNS.length + 1) - 1;
    }

    // Drawn in the background pass so the buttons, which the base class
    // extracts afterwards, end up on top of the panel instead of under it.
    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);

        int panelWidth = PANEL_PAD + GRID + PANEL_PAD + INFO_WIDTH + PANEL_PAD;
        int panelHeight = PANEL_PAD + 24 + GRID + PANEL_PAD;
        extractor.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2,
                0xF0100C18);
        extractor.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1C1426);

        if (patternIndex == OVERVIEW) {
            extractOverview(extractor, mouseX, mouseY);
            return;
        }
        TropicalFish.Pattern pattern = PATTERNS[patternIndex];

        // header: pattern name centered over the grid, between the arrows
        Component header = Component.literal(
                niceName(pattern.getSerializedName()) + "  (" + (patternIndex + 1) + "/"
                        + PATTERNS.length + ")");
        extractor.text(this.font, header.getVisualOrderText(),
                gridX + (GRID - this.font.width(header)) / 2, panelY + PANEL_PAD + 4,
                0xFFFFFFFF, true);

        // the 16x16 grid: rows = base color, columns = pattern color. All
        // 16 dye colors: black spawns naturally on Java since 1.19.3.
        int patternCollected = 0;
        for (int row = 0; row < COLORS.length; row++) {
            for (int col = 0; col < COLORS.length; col++) {
                int packed = new TropicalFish.Variant(pattern, COLORS[row], COLORS[col])
                        .getPackedId();
                int x = gridX + col * CELL;
                int y = gridY + row * CELL;
                boolean collected = FishCollection.isCollected(packed);
                if (collected) {
                    patternCollected++;
                    int base = COLORS[row].getTextureDiffuseColor();
                    int pat = COLORS[col].getTextureDiffuseColor();
                    // left half base color, right half pattern color,
                    // split dead center so neither color gets squeezed
                    extractor.fill(x + 1, y + 1, x + CELL / 2, y + CELL - 1, base);
                    extractor.fill(x + CELL / 2, y + 1, x + CELL - 1, y + CELL - 1, pat);
                } else {
                    extractor.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0xFF2A2438);
                }

                // markers: white dot = one of the 22 commons, purple dot =
                // solid (the base/pattern color diagonal)
                if (COMMON_INDEX.containsKey(packed)) {
                    extractor.fill(x + CELL - 4, y + 2, x + CELL - 2, y + 4, 0xFFFFFFFF);
                }
                if (row == col) {
                    extractor.fill(x + 2, y + CELL - 4, x + 4, y + CELL - 2, 0xFFFF77FF);
                }
            }
        }

        // hover: highlight the cell and show that variant in the info panel
        int hoverCol = (mouseX - gridX) / CELL;
        int hoverRow = (mouseY - gridY) / CELL;
        boolean hovering = mouseX >= gridX && mouseY >= gridY
                && hoverCol >= 0 && hoverCol < COLORS.length
                && hoverRow >= 0 && hoverRow < COLORS.length;

        int infoX = gridX + GRID + PANEL_PAD;
        int infoY = gridY;

        if (hovering) {
            int hx = gridX + hoverCol * CELL;
            int hy = gridY + hoverRow * CELL;
            extractor.fill(hx, hy, hx + CELL, hy + 1, 0xFFFFFFFF);
            extractor.fill(hx, hy + CELL - 1, hx + CELL, hy + CELL, 0xFFFFFFFF);
            extractor.fill(hx, hy, hx + 1, hy + CELL, 0xFFFFFFFF);
            extractor.fill(hx + CELL - 1, hy, hx + CELL, hy + CELL, 0xFFFFFFFF);

            int packed = new TropicalFish.Variant(pattern, COLORS[hoverRow], COLORS[hoverCol])
                    .getPackedId();
            boolean collected = FishCollection.isCollected(packed);

            // fish preview only for collected variants: seeing one swim by
            // is not owning it, the model is the reward for the bucket
            if (collected) {
                TropicalFishConfig config = TropicalFishConfig.get();
                FishTooltipRenderer.extractFish(extractor, packed,
                        infoX, infoY, infoX + INFO_WIDTH, infoY + 54, 48,
                        config.tooltipFishYaw, config.tooltipFishTilt);
            } else {
                Component unknown = Component.literal("?");
                extractor.text(this.font, unknown.getVisualOrderText(),
                        infoX + (INFO_WIDTH - this.font.width(unknown)) / 2, infoY + 24,
                        0xFF666666, true);
            }

            int line = infoY + 58;
            Integer commonIndex = COMMON_INDEX.get(packed);
            if (commonIndex != null) {
                line = infoLine(extractor, infoX, line,
                        Component.translatable(
                                "entity.minecraft.tropical_fish.predefined." + commonIndex)
                                .getString(), 0xFFFFFFFF);
            }
            line = infoLine(extractor, infoX, line,
                    niceName(COLORS[hoverRow].getName()) + " / "
                            + niceName(COLORS[hoverCol].getName()),
                    commonIndex != null ? 0xFFAAAAAA : 0xFFFFFFFF);
            line = infoLine(extractor, infoX, line,
                    "No. " + FishCollection.numberOf(packed) + " of " + FishCollection.TOTAL_VARIANTS,
                    0xFF888888);
            String state = collected ? "Collected" : "Not collected";
            int stateColor = collected ? 0xFF55FF55 : 0xFF888888;
            line = infoLine(extractor, infoX, line, state, stateColor);
            int catches = FishCollection.catches(packed);
            if (catches > 0) {
                line = infoLine(extractor, infoX, line,
                        "Caught " + catches + (catches == 1 ? " time" : " times"), 0xFFAAAAAA);
            }
        }

        // legend when nothing is hovered
        if (!hovering) {
            extractor.fill(infoX, infoY + 3, infoX + 4, infoY + 7, 0xFFFFFFFF);
            infoLine(extractor, infoX + 8, infoY + 1, "Common (has a name)", 0xFFAAAAAA);
            extractor.fill(infoX, infoY + 14, infoX + 4, infoY + 18, 0xFFFF77FF);
            infoLine(extractor, infoX + 8, infoY + 12, "Solid (matching colors)", 0xFFAAAAAA);
        }

        // progress block, bottom of the info column, clear of the Done button
        int progressY = gridY + GRID - 80;
        infoLine(extractor, infoX, progressY,
                "This pattern: " + patternCollected + "/" + COLORS.length * COLORS.length,
                0xFFAAAAFF);
        infoLine(extractor, infoX, progressY + 11,
                "Total: " + FishCollection.collectedTotal() + "/" + FishCollection.TOTAL_VARIANTS,
                0xFFAAAAFF);
        infoLine(extractor, infoX, progressY + 22,
                "Commons: " + FishCollection.collectedCommons() + "/"
                        + TropicalFish.COMMON_VARIANTS.size(), 0xFFFFFFFF);
        infoLine(extractor, infoX, progressY + 33,
                "Solids: " + FishCollection.collectedSolids() + "/"
                        + PATTERNS.length * COLORS.length, 0xFFFF77FF);
        infoLine(extractor, infoX, progressY + 44,
                "Catches: " + FishCollection.totalCatches(), 0xFFAAAAFF);
    }

    // ------------------------------------------------------------------
    // Overview: the front page. All 12 patterns with a live poster fish,
    // progress and a bar; click one to open its grid. What the advancement
    // "fishdex" datapacks build 13 tabs and a resource pack for, without
    // needing either or any server-side install.
    // ------------------------------------------------------------------

    private static final int OV_COLS = 3;
    private static final int OV_COL_W = (GRID + PANEL_PAD + INFO_WIDTH) / OV_COLS;
    private static final int OV_ROW_H = GRID / 4;

    private void extractOverview(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int full = COLORS.length * COLORS.length;
        int total = FishCollection.collectedTotal();
        boolean dexComplete = total == FishCollection.TOTAL_VARIANTS;

        Component header = Component.literal(dexComplete ? "Fish Collection - COMPLETE" : "Fish Collection");
        extractor.text(this.font, header.getVisualOrderText(),
                gridX + (GRID + PANEL_PAD + INFO_WIDTH - this.font.width(header)) / 2,
                panelY + PANEL_PAD + 4, dexComplete ? 0xFFFFD700 : 0xFFFFFFFF, true);

        TropicalFishConfig config = TropicalFishConfig.get();
        for (int i = 0; i < PATTERNS.length; i++) {
            int ex = gridX + (i % OV_COLS) * OV_COL_W;
            int ey = gridY + (i / OV_COLS) * OV_ROW_H;
            boolean hover = mouseX >= ex && mouseX < ex + OV_COL_W - 2
                    && mouseY >= ey && mouseY < ey + OV_ROW_H - 2;
            extractor.fill(ex, ey, ex + OV_COL_W - 2, ey + OV_ROW_H - 2,
                    hover ? 0xFF2E2542 : 0xFF241D33);

            int collected = FishCollection.patternCollected(PATTERNS[i]);
            int poster = FishCollection.firstCollected(PATTERNS[i]);
            if (poster >= 0) {
                FishTooltipRenderer.extractFish(extractor, poster,
                        ex + 2, ey + 2, ex + 40, ey + OV_ROW_H - 4, 22,
                        config.tooltipFishYaw, config.tooltipFishTilt);
            } else {
                Component unknown = Component.literal("?");
                extractor.text(this.font, unknown.getVisualOrderText(),
                        ex + 18, ey + OV_ROW_H / 2 - 5, 0xFF666666, true);
            }

            boolean done = collected == full;
            infoLine(extractor, ex + 42, ey + 6, niceName(PATTERNS[i].getSerializedName()),
                    done ? 0xFFFFD700 : 0xFFFFFFFF);
            infoLine(extractor, ex + 42, ey + 17, collected + "/" + full,
                    done ? 0xFFFFD700 : 0xFFAAAAAA);
            int barW = OV_COL_W - 48;
            int fillW = barW * collected / full;
            extractor.fill(ex + 42, ey + 30, ex + 42 + barW, ey + 34, 0xFF13101C);
            if (fillW > 0) {
                extractor.fill(ex + 42, ey + 30, ex + 42 + fillW, ey + 34,
                        done ? 0xFFFFD700 : 0xFF55FF55);
            }
        }

        infoLine(extractor, gridX,
                gridY + GRID + 2 - 11,
                "Total: " + total + "/" + FishCollection.TOTAL_VARIANTS
                        + "   Commons: " + FishCollection.collectedCommons() + "/"
                        + TropicalFish.COMMON_VARIANTS.size()
                        + "   Solids: " + FishCollection.collectedSolids() + "/"
                        + PATTERNS.length * COLORS.length
                        + "   Catches: " + FishCollection.totalCatches(),
                0xFFAAAAFF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (patternIndex == OVERVIEW) {
            int mx = (int) event.x();
            int my = (int) event.y();
            if (mx >= gridX && my >= gridY && my < gridY + 4 * OV_ROW_H) {
                int col = (mx - gridX) / OV_COL_W;
                int row = (my - gridY) / OV_ROW_H;
                int i = row * OV_COLS + col;
                if (col < OV_COLS && i >= 0 && i < PATTERNS.length) {
                    patternIndex = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int infoLine(GuiGraphicsExtractor extractor, int x, int y, String text, int color) {
        extractor.text(this.font, Component.literal(text).getVisualOrderText(), x, y, color, true);
        return y + 11;
    }

    private static int dim(int argb) {
        int r = (argb >> 16 & 0xFF) * 35 / 100;
        int g = (argb >> 8 & 0xFF) * 35 / 100;
        int b = (argb & 0xFF) * 35 / 100;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static String niceName(String serialized) {
        String spaced = serialized.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
