package com.rasmus.rarefishfinder.client;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;

/**
 * Version-agnostic way to reach the ToastManager.
 *
 * <p>26.1 hangs it off Minecraft (`Minecraft.getToastManager()`); 26.2 moved
 * it to the Gui instance (`Minecraft.gui.toastManager()`) and dropped the
 * Minecraft accessor entirely. Neither name exists on both, and a direct
 * reference to either is a NoSuchMethodError on the other version, so the
 * lookup happens once by name exactly like the `Minecraft.screen` split.
 *
 * <p>ToastManager itself, and `addToast`, are identical on both, so only
 * getting hold of the instance needs the reflection.
 */
public final class Toasts {

    private Toasts() {
    }

    private static Method minecraftAccessor;
    private static Method guiAccessor;
    private static boolean lookupFailed;

    /** Show a toast, or do nothing if this runtime exposes neither accessor. */
    public static void show(Toast toast) {
        ToastManager manager = manager();
        if (manager != null) {
            manager.addToast(toast);
        }
    }

    private static ToastManager manager() {
        if (lookupFailed) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        try {
            if (minecraftAccessor == null && guiAccessor == null) {
                try {
                    minecraftAccessor = Minecraft.class.getMethod("getToastManager");
                } catch (NoSuchMethodException e) {
                    guiAccessor = client.gui.getClass().getMethod("toastManager");
                }
            }
            Object manager = minecraftAccessor != null
                    ? minecraftAccessor.invoke(client)
                    : guiAccessor.invoke(client.gui);
            return (ToastManager) manager;
        } catch (ReflectiveOperationException | ClassCastException e) {
            // A toast is cosmetic: losing it is not worth taking the catch
            // handler (and the caught fish) down with it.
            lookupFailed = true;
            return null;
        }
    }
}
