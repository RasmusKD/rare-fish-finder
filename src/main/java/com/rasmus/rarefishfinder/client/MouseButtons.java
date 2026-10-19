package com.rasmus.rarefishfinder.client;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * 26.1 and 26.2 number the left mouse button 0, 26.3 numbers it 1. The constant is read at runtime
 * so the value is not inlined at compile time into a jar that must run on all three.
 */
public final class MouseButtons {
    private static final int LEFT = resolveLeft();

    private MouseButtons() {
    }

    private static int resolveLeft() {
        try {
            return InputConstants.class.getField("MOUSE_BUTTON_LEFT").getInt(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0;
        }
    }

    public static boolean isLeft(int button) {
        return button == LEFT;
    }
}
