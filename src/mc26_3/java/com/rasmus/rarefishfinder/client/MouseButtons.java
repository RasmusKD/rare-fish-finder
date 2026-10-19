package com.rasmus.rarefishfinder.client;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * 26.3 numbers the left mouse button 1. The constant is inlined at compile time, so this
 * 26.3-only jar carries 26.3's value.
 */
public final class MouseButtons {

    private MouseButtons() {
    }

    public static boolean isLeft(int button) {
        return button == InputConstants.MOUSE_BUTTON_LEFT;
    }
}
