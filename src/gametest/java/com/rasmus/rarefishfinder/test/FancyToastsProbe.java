package com.rasmus.rarefishfinder.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.util.FormattedCharSequence;

/** What the probe mixins saw Fancy Toasts build: each toast's banner text and toast sound. */
public final class FancyToastsProbe {
    public static final List<String> BANNERS = new CopyOnWriteArrayList<>();
    public static final List<String> SOUNDS = new CopyOnWriteArrayList<>();

    private FancyToastsProbe() {
    }

    public static String text(List<FormattedCharSequence> lines) {
        StringBuilder sb = new StringBuilder();
        for (FormattedCharSequence line : lines) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            line.accept((index, style, codePoint) -> {
                sb.appendCodePoint(codePoint);
                return true;
            });
        }
        return sb.toString();
    }
}
