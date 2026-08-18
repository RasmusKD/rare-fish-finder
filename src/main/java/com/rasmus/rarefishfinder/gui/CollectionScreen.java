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

    private int patternIndex;
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
        patternIndex = Math.floorMod(patternIndex + direction, PATTERNS.length);
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
                boolean spotted = FishCollection.isSpotted(packed);
                if (collected) {
                    patternCollected++;
                }

                if (collected || spotted) {
                    int base = COLORS[row].getTextureDiffuseColor();
                    int pat = COLORS[col].getTextureDiffuseColor();
                    if (!collected) {
                        base = dim(base);
                        pat = dim(pat);
                    }
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
            boolean spotted = FishCollection.isSpotted(packed);

            // fish preview, only for variants that have at least been spotted:
            // no spoilers on what an unseen combination looks like
            if (collected || spotted) {
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
            String state = collected ? "Collected" : (spotted ? "Spotted" : "Not seen");
            int stateColor = collected ? 0xFF55FF55 : (spotted ? 0xFFFFFF55 : 0xFF888888);
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
