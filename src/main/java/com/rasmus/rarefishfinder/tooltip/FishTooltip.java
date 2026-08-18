package com.rasmus.rarefishfinder.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip data for a tropical fish bucket. The stack carries the variant
 * components; the client renderer turns it into a posed fish model.
 */
public record FishTooltip(ItemStack bucket) implements TooltipComponent {
}
