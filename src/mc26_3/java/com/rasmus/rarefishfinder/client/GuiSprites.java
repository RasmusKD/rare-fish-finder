package com.rasmus.rarefishfinder.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Sprite and texture blits for the 26.3 jar, called directly: 26.3 moved
 * RenderPipeline to `com.mojang.renderpearl.api.pipeline`, and this jar is
 * compiled against it. The 26.1/26.2 jar resolves the same calls by reflection.
 */
public final class GuiSprites {

    private GuiSprites() {
    }

    public static void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite,
            int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    public static void blit(GuiGraphicsExtractor graphics, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                textureWidth, textureHeight);
    }
}
