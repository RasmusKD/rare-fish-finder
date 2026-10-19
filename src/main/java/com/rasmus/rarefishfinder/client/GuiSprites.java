package com.rasmus.rarefishfinder.client;

import java.lang.reflect.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Sprite and texture blits for the 26.1/26.2 jar. The 26.3 jar replaces this class with direct calls
 * (src/mc26_3), because 26.3 moved RenderPipeline to `com.mojang.renderpearl.api.pipeline`.
 *
 * <p>Here the field and both methods are resolved once by name and parameter shape; the call sites stay
 * the same in both jars.
 */public final class GuiSprites {

    private GuiSprites() {
    }

    private static Object pipeline;
    private static Method spriteMethod;
    private static Method blitMethod;
    private static boolean lookupFailed;

    public static void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite,
            int x, int y, int width, int height) {
        if (!resolve()) {
            return;
        }
        try {
            spriteMethod.invoke(graphics, pipeline, sprite, x, y, width, height);
        } catch (ReflectiveOperationException e) {
            lookupFailed = true;
        }
    }

    public static void blit(GuiGraphicsExtractor graphics, Identifier texture,
            int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        if (!resolve()) {
            return;
        }
        try {
            blitMethod.invoke(graphics, pipeline, texture, x, y, u, v, width, height,
                    textureWidth, textureHeight);
        } catch (ReflectiveOperationException e) {
            lookupFailed = true;
        }
    }

    private static boolean resolve() {
        if (lookupFailed) {
            return false;
        }
        if (pipeline != null) {
            return true;
        }
        try {
            Object found = RenderPipelines.class.getField("GUI_TEXTURED").get(null);
            for (Method method : GuiGraphicsExtractor.class.getMethods()) {
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 0 || !types[0].isInstance(found)) {
                    continue;
                }
                if (method.getName().equals("blitSprite") && types.length == 6
                        && types[1] == Identifier.class) {
                    spriteMethod = method;
                } else if (method.getName().equals("blit") && types.length == 10
                        && types[1] == Identifier.class && types[4] == float.class) {
                    blitMethod = method;
                }
            }
            if (spriteMethod == null || blitMethod == null) {
                lookupFailed = true;
                return false;
            }
            pipeline = found;
            return true;
        } catch (ReflectiveOperationException e) {
            lookupFailed = true;
            return false;
        }
    }
}
