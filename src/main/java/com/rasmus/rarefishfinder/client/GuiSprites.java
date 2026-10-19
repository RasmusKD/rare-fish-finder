package com.rasmus.rarefishfinder.client;

import java.lang.reflect.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Sprite and texture blits that survive the 26.3 move of RenderPipeline.
 *
 * <p>26.1 and 26.2 declare the pipeline type in `com.mojang.blaze3d.pipeline`;
 * 26.3 moved it to `com.mojang.renderpearl.api.pipeline`. That changes the
 * descriptor of `RenderPipelines.GUI_TEXTURED`, `blitSprite` and `blit`, so a
 * direct call compiled on one version is a NoSuchFieldError/NoSuchMethodError
 * on the other. The field and both methods are resolved once by name and
 * parameter shape instead; the call sites stay the same on every version.
 */
public final class GuiSprites {

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
