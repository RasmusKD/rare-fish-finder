package com.rasmus.rarefishfinder.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

@Config(name = "rarefishfinder")
public class TropicalFishConfig implements ConfigData {

    public enum GlowMode {
        OFF, RARE, ALL;

        @Override
        public String toString() {
            return switch (this) {
                case OFF -> "Off";
                case RARE -> "Rare fish";
                case ALL -> "All tropical fish";
            };
        }
    }

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    @ConfigEntry.Gui.Tooltip
    public GlowMode glowMode = GlowMode.RARE;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean glowOnlyUncollected = false;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean newCatchToasts = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean fancyToastsStyle = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean namesEnabled = true;

    @ConfigEntry.Category("tooltip")
    @ConfigEntry.Gui.Tooltip
    public boolean showBucketColors = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean sortFishBuckets = true;

    @ConfigEntry.Category("fixes")
    @ConfigEntry.Gui.Tooltip
    public boolean fixFinLines = true;

    @ConfigEntry.Category("tooltip")
    @ConfigEntry.Gui.Tooltip
    public boolean showFishInTooltip = true;

    @ConfigEntry.Category("tooltip")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 359)
    @ConfigEntry.Gui.Tooltip
    public int tooltipFishYaw = 135;

    @ConfigEntry.Category("tooltip")
    @ConfigEntry.BoundedDiscrete(min = -45, max = 45)
    @ConfigEntry.Gui.Tooltip
    public int tooltipFishTilt = -45;

    @ConfigEntry.Category("general")
    @ConfigEntry.ColorPicker
    @ConfigEntry.Gui.Tooltip
    public int glowColor = 0xFFFFFF;

    @ConfigEntry.Category("collection")
    @ConfigEntry.Gui.Tooltip
    public boolean hoverCollects = true;

    @ConfigEntry.Category("collection")
    @ConfigEntry.Gui.Tooltip
    public boolean showNewBadge = true;

    @ConfigEntry.Category("xaero")
    @ConfigEntry.Gui.Tooltip
    public boolean hideCommonFishOnXaeroMap = false;

    @ConfigEntry.Category("xaero")
    @ConfigEntry.Gui.Tooltip
    public boolean onlyTropicalFishOnXaeroMap = false;

    public static void register() {
        AutoConfig.register(TropicalFishConfig.class, GsonConfigSerializer::new);
    }

    public static TropicalFishConfig get() {
        return AutoConfig.getConfigHolder(TropicalFishConfig.class).getConfig();
    }
}