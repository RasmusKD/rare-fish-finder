package com.rasmus.rarefishfinder;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Version-gated mixin pairs. Both pairs split on the same question, so they
 * share one predicate:
 *
 * <ul>
 *   <li>26.1 calls the HUD class Gui; 26.2 renamed it Hud but kept a
 *       different Gui class around, so class presence cannot tell them apart
 *       and the version string does.</li>
 *   <li>AdvancementsScreen's extract methods carry the window origin on 26.1
 *       and not on 26.2, which is a descriptor difference: applying the wrong
 *       one of the pair is a hard crash at mixin apply, not a soft failure.</li>
 * </ul>
 *
 * <p>Anything gated here needs BOTH halves listed in the mixin config; a half
 * that is never selected is dead, and a half that is always selected defeats
 * the gate.
 */
public class RareFishFinderMixinPlugin implements IMixinConfigPlugin {

    private static boolean isLegacyGui() {
        String version = FabricLoader.getInstance().getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
        return version.startsWith("26.1");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("LegacyMixin")) {
            return isLegacyGui();
        }
        if (mixinClassName.endsWith("ModernMixin")) {
            return !isLegacyGui();
        }
        if (mixinClassName.endsWith("BetterAdvancementsScreenMixin")) {
            // Its target class ships with an optional mod. Mixin would abort
            // the whole config on a missing target, taking every other mixin
            // here down with it, so this asks before it is looked up.
            return FabricLoader.getInstance().isModLoaded("betteradvancements");
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
            IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
            IMixinInfo mixinInfo) {
    }
}
