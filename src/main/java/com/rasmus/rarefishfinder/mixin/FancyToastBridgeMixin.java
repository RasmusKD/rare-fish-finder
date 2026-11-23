package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.gui.SyntheticAdvancementToasts;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes Fancy Toasts show our synthetic catch toasts (SyntheticAdvancementToasts)
 * the way our own popup does. Their animation draws two things we replace:
 *
 * <ul>
 *   <li>The icon is an item stack, so our toasts would carry a bucket. The one
 *       fakeItem call in their icon draw renders the live fish instead, at
 *       whatever position their animation moved the icon to (the extractor's
 *       pose carries their transform, same trick as our own toasts).</li>
 *   <li>The banner line is the advancement type's name ("Goal Reached!"), so a
 *       catch lost whether it was rare. setLines gets our banner instead.</li>
 * </ul>
 *
 * <p>Their display object changed between releases (1.4.7 for 26.1 holds a
 * ToastDisplayInfo in the field displayInfo, 1.5.1 for 26.2 an
 * AdvancementDisplay in the field display); both have getTitle(), which is
 * all this needs, and both keep setLines(Component, Component) and the
 * fakeItem call. Their classes are referenced only by name and reflection,
 * and both hooks are require = 0: a future Fancy Toasts release that moves
 * them leaves vanilla-looking Fancy toasts, never a crash at startup.
 */
@Pseudo
@Mixin(targets = "net.bivrik.fancytoasts.client.toast.animation.FancyToastAnimation", remap = false)
public abstract class FancyToastBridgeMixin {

    @Unique
    private static final String[] RFF_DISPLAY_FIELDS = {"display", "displayInfo"};

    @Unique
    private static Field rff$displayField;
    @Unique
    private static Method rff$getTitle;
    @Unique
    private static boolean rff$reflectionFailed;

    @ModifyVariable(method = "setLines", at = @At("HEAD"), argsOnly = true, ordinal = 0,
            remap = false, require = 0)
    private Component rff$ourBanner(Component banner) {
        SyntheticAdvancementToasts.Tag tag = rff$tag();
        return tag != null && tag.banner() != null ? tag.banner() : banner;
    }

    @Redirect(method = "drawIcon(Lnet/bivrik/fancytoasts/platform/utility/GuiContext;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V"),
            remap = false, require = 0)
    private void rff$fishInsteadOfBucket(GuiGraphicsExtractor graphics, ItemStack icon, int x, int y) {
        SyntheticAdvancementToasts.Tag tag = rff$tag();
        if (tag == null || tag.packed() < 0) {
            graphics.fakeItem(icon, x, y);
            return;
        }
        var pose = graphics.pose();
        int ox = Math.round(pose.m20);
        int oy = Math.round(pose.m21);
        // Their frame is 26x26 with the 16px item at (x, y) = its center + 5;
        // the fish fills the frame interior instead.
        TropicalFishConfig config = TropicalFishConfig.get();
        FishTooltipRenderer.extractFish(graphics, tag.packed(),
                ox + x - 4, oy + y - 4, ox + x + 20, oy + y + 20, 20,
                config.tooltipFishYaw, config.tooltipFishTilt);
    }

    /** The tag when this animation shows one of our toasts, else null. */
    @Unique
    private SyntheticAdvancementToasts.@Nullable Tag rff$tag() {
        if (rff$reflectionFailed) {
            return null;
        }
        try {
            if (rff$displayField == null) {
                Field field = rff$findDisplayField(getClass());
                if (field == null) {
                    rff$reflectionFailed = true;
                    return null;
                }
                field.setAccessible(true);
                rff$getTitle = field.getType().getMethod("getTitle");
                rff$displayField = field;
            }
            Object display = rff$displayField.get(this);
            if (display == null) {
                return null;
            }
            return SyntheticAdvancementToasts.tagFor((Component) rff$getTitle.invoke(display));
        } catch (ReflectiveOperationException | RuntimeException e) {
            rff$reflectionFailed = true;
            return null;
        }
    }

    /** Walks up from the concrete animation class to the one that declares the display field. */
    @Unique
    private static @Nullable Field rff$findDisplayField(Class<?> start) {
        for (Class<?> c = start; c != null; c = c.getSuperclass()) {
            for (String name : RFF_DISPLAY_FIELDS) {
                try {
                    return c.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    // try the next name, then the superclass
                }
            }
        }
        return null;
    }
}
