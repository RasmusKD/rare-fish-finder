package com.rasmus.rarefishfinder.test.mixin;

import com.rasmus.rarefishfinder.test.FancyToastsProbe;
import java.lang.reflect.Field;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records which sound each Fancy Toasts toast will play (it picks it from the advancement type). */
@Pseudo
@Mixin(targets = "net.bivrik.fancytoasts.client.toast.FancyAdvancementToast", remap = false)
public abstract class FancyToastSoundProbeMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void rffTest$recordSound(CallbackInfo ci) throws ReflectiveOperationException {
        Field field = getClass().getDeclaredField("toastSoundId");
        field.setAccessible(true);
        FancyToastsProbe.SOUNDS.add(String.valueOf(field.get(this)));
    }
}
