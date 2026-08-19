package com.rasmus.rarefishfinder.client;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import java.lang.reflect.Field;
import net.minecraft.client.model.geom.builders.CubeDeformation;

/**
 * Helpers for the fin line fix. The base layer is baked with
 * CubeDeformation.NONE and must stay untouched; only the inflated pattern
 * layer gets rebuilt. The grow fields are package private, so they are read
 * once by reflection.
 */
public final class FinLineFix {

    private static Field growX;
    private static Field growY;
    private static Field growZ;

    private FinLineFix() {
    }

    public static boolean shouldFix(CubeDeformation g) {
        return g != CubeDeformation.NONE && TropicalFishConfig.get().fixFinLines;
    }

    public static CubeDeformation withoutX(CubeDeformation g) {
        return new CubeDeformation(0.0F, grow(g, 1), grow(g, 2));
    }

    public static CubeDeformation withoutZ(CubeDeformation g) {
        return new CubeDeformation(grow(g, 0), grow(g, 1), 0.0F);
    }

    private static float grow(CubeDeformation g, int axis) {
        try {
            if (growX == null) {
                growX = CubeDeformation.class.getDeclaredField("growX");
                growY = CubeDeformation.class.getDeclaredField("growY");
                growZ = CubeDeformation.class.getDeclaredField("growZ");
                growX.setAccessible(true);
                growY.setAccessible(true);
                growZ.setAccessible(true);
            }
            return switch (axis) {
                case 0 -> growX.getFloat(g);
                case 1 -> growY.getFloat(g);
                default -> growZ.getFloat(g);
            };
        } catch (ReflectiveOperationException e) {
            // the vanilla pattern layer inflation, the only non-NONE caller
            return 0.008F;
        }
    }
}
