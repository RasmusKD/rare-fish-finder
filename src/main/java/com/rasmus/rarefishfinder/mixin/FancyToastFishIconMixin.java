package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fancy Toasts shows advancement icons as item stacks, which made our
 * synthetic catch toasts carry a bucket. This redirects the one fakeItem
 * call in their icon draw: for our own toasts the live fish model renders
 * inside their frame instead, at whatever position their animation moved
 * the icon to (the extractor's pose carries their transform, same trick as
 * our own toasts). Everything that is not ours falls through to the
 * original call untouched. Their classes are referenced only by name and
 * reflection, so the mixin simply never applies when the mod is absent.
 */
@Mixin(targets = "net.bivrik.fancytoasts.client.toast.animation.FancyToastAnimation", remap = false)
public abstract class FancyToastFishIconMixin {

    @Unique
    private static Field rff$displayInfoField;
    @Unique
    private static Method rff$getTitle;
    @Unique
    private static Method rff$getDescription;
    @Unique
    private static boolean rff$reflectionFailed;

    @Redirect(method = "drawIcon(Lnet/bivrik/fancytoasts/platform/utility/GuiContext;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V"),
            remap = false)
    private void rff$fishInsteadOfBucket(GuiGraphicsExtractor graphics, ItemStack icon, int x, int y) {
        int packed = rff$ownedPacked();
        if (packed < 0) {
            graphics.fakeItem(icon, x, y);
            return;
        }
        var pose = graphics.pose();
        int ox = Math.round(pose.m20);
        int oy = Math.round(pose.m21);
        // Their frame is 26x26 with the 16px item at (x, y) = its center + 5;
        // the fish fills the frame interior instead.
        TropicalFishConfig config = TropicalFishConfig.get();
        FishTooltipRenderer.extractFish(graphics, packed,
                ox + x - 4, oy + y - 4, ox + x + 20, oy + y + 20, 20,
                config.tooltipFishYaw, config.tooltipFishTilt);
    }

    /**
     * The packed variant when this animation shows one of our toasts, else
     * -1. Recognition: the title is one of our translation keys, and the
     * description leads with the collection number, which inverts to the
     * variant (the numbering is a bijection).
     */
    @Unique
    private int rff$ownedPacked() {
        if (rff$reflectionFailed) {
            return -1;
        }
        try {
            if (rff$displayInfoField == null) {
                // walk up to the class that declares displayInfo
                Class<?> c = getClass();
                Field field = null;
                while (c != null && field == null) {
                    try {
                        field = c.getDeclaredField("displayInfo");
                    } catch (NoSuchFieldException e) {
                        c = c.getSuperclass();
                    }
                }
                if (field == null) {
                    rff$reflectionFailed = true;
                    return -1;
                }
                field.setAccessible(true);
                rff$displayInfoField = field;
                Class<?> infoType = field.getType();
                rff$getTitle = infoType.getMethod("getTitle");
                rff$getDescription = infoType.getMethod("getDescription");
            }
            Object info = rff$displayInfoField.get(this);
            if (info == null) {
                return -1;
            }
            Component title = (Component) rff$getTitle.invoke(info);
            if (!(title.getContents() instanceof TranslatableContents translatable)
                    || !translatable.getKey().startsWith("toast.rarefishfinder.")) {
                return -1;
            }
            Component description = (Component) rff$getDescription.invoke(info);
            String text = description.getString();
            if (!text.startsWith("#")) {
                return -1;
            }
            int end = 1;
            while (end < text.length() && Character.isDigit(text.charAt(end))) {
                end++;
            }
            if (end == 1) {
                return -1;
            }
            return FishCollection.packedFromNumber(Integer.parseInt(text.substring(1, end)));
        } catch (ReflectiveOperationException | RuntimeException e) {
            rff$reflectionFailed = true;
            return -1;
        }
    }
}
