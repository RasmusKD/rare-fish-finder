package com.rasmus.rarefishfinder.client;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Mouse button checks by name. 26.1/26.2 number left 0 and right 1, 26.3 numbers them 1 and 3;
 * the constants are inlined at compile time, so each jar carries its own values.
 */
public final class MouseButtons {

    private MouseButtons() {
    }

    public static boolean isLeft(int button) {
        return button == InputConstants.MOUSE_BUTTON_LEFT;
    }

    public static boolean isRight(int button) {
        return button == InputConstants.MOUSE_BUTTON_RIGHT;
    }
}
