package com.rasmus.rarefishfinder;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * The 26.3 jar has no Gui/Hud or AdvancementsScreen split to choose between:
 * only the Modern mixins of the pairs are compiled into it. The one remaining
 * gate is the optional Better Advancements target.
 */
public class RareFishFinderMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
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
