package com.rasmus.rarefishfinder.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "rarefishfinder")
public class TropicalFishConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean glowEnabled = true;

    @ConfigEntry.Gui.Tooltip
    public boolean namesEnabled = true;

    public static void register() {
        AutoConfig.register(TropicalFishConfig.class, GsonConfigSerializer::new);
    }

    public static TropicalFishConfig get() {
        return AutoConfig.getConfigHolder(TropicalFishConfig.class).getConfig();
    }
}