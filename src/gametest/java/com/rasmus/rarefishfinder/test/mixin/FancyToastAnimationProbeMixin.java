package com.rasmus.rarefishfinder.test.mixin;

import com.rasmus.rarefishfinder.test.FancyToastsProbe;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the banner line Fancy Toasts settled on, after every mod's changes. */
@Pseudo
@Mixin(targets = "net.bivrik.fancytoasts.client.toast.animation.FancyToastAnimation", remap = false)
public abstract class FancyToastAnimationProbeMixin {

    @Inject(method = "setLines", at = @At("TAIL"), remap = false)
    @SuppressWarnings("unchecked")
    private void rffTest$recordBanner(CallbackInfo ci) throws ReflectiveOperationException {
        Class<?> c = getClass();
        while (!c.getName().endsWith(".FancyToastAnimation")) {
            c = c.getSuperclass();
        }
        Field field = c.getDeclaredField("titleLines");
        field.setAccessible(true);
        FancyToastsProbe.BANNERS.add(FancyToastsProbe.text((List<FormattedCharSequence>) field.get(this)));
    }
}
