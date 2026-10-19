package com.rasmus.rarefishfinder.client;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * 26.1 and 26.2 number the left mouse button 0. The 26.3 jar replaces this class (it numbers the button 1;
 * see src/mc26_3).
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
