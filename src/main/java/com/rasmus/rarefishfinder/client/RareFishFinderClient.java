package com.rasmus.rarefishfinder.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.gui.CollectionScreen;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import com.rasmus.rarefishfinder.tooltip.FishTooltip;
import com.rasmus.rarefishfinder.tooltip.FishTooltipRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.Items;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RareFishFinderClient implements ClientModInitializer {
    public static final String MOD_ID = "rarefishfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping toggleGlowKeyBinding;
    private static KeyMapping toggleNamesKeyBinding;
    private static KeyMapping collectionKeyBinding;

    private static final KeyMapping.Category RAREFISH_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("rarefishfinder", "category"));

    // 26.1 exposes the current screen as the public field Minecraft.screen,
    // 26.2 as the method Minecraft.screen(). A direct reference to either is
    // a NoSuchFieldError/NoSuchMethodError on the other version, so the
    // lookup happens once by name.
    private static Method screenMethod;
    private static Field screenField;
    private static boolean screenLookupFailed;

    private static boolean noScreenOpen(Minecraft client) {
        if (screenLookupFailed) {
            return true;
        }
        try {
            if (screenMethod == null && screenField == null) {
                try {
                    screenMethod = Minecraft.class.getMethod("screen");
                } catch (NoSuchMethodException e) {
                    screenField = Minecraft.class.getField("screen");
                }
            }
            Object screen = screenMethod != null
                    ? screenMethod.invoke(client) : screenField.get(client);
            return screen == null;
        } catch (ReflectiveOperationException e) {
            screenLookupFailed = true;
            return true;
        }
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Rare Fish Finder initialized!");
        ClientSortCompat.init();
        MouseWheelieCompat.init();

        // The Xaero map option only exists when the map does: without the mod
        // the mixin never applies, so showing the toggle would sell a no-op.
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            me.shedaniel.autoconfig.AutoConfigClient.getGuiRegistry(TropicalFishConfig.class).registerPredicateProvider(
                    (i13n, field, config, defaults, registry) -> java.util.Collections.emptyList(),
                    field -> field.getName().equals("hideCommonFishOnXaeroMap")
                            || field.getName().equals("onlyTropicalFishOnXaeroMap"));
        }

        // A water bucket used on a tropical fish is this client's own catch:
        // count it and mark the variant collected.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (entity instanceof TropicalFish fish
                    && player.getItemInHand(hand).is(Items.WATER_BUCKET)) {
                // Our name tag is a spotting aid; without this strip it
                // follows the fish into the bucket item's custom name and
                // duplicates what the tooltip already shows. Runs on both
                // sides so the singleplayer server copy is stripped too.
                if (fish.hasCustomName()) {
                    fish.setCustomName(null);
                    fish.setCustomNameVisible(false);
                }
                if (level.isClientSide()) {
                    FishCollection.addCatch(FishCollection.packedOf(fish));
                }
            }
            return InteractionResult.PASS;
        });

        ClientTooltipComponentCallback.EVENT.register(component ->
                component instanceof FishTooltip fishTooltip
                        ? new FishTooltipRenderer(fishTooltip) : null);

        toggleGlowKeyBinding = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.rarefishfinder.toggleGlow",
                        InputConstants.Type.KEYSYM,
                        82, // R key (for Rare fish)
                        RAREFISH_CATEGORY
                )
        );

        toggleNamesKeyBinding = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.rarefishfinder.toggleNames",
                        InputConstants.Type.KEYSYM,
                        78, // N key
                        RAREFISH_CATEGORY
                )
        );

        collectionKeyBinding = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.rarefishfinder.collection",
                        InputConstants.Type.KEYSYM,
                        66, // B key
                        RAREFISH_CATEGORY
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (collectionKeyBinding.consumeClick()) {
                if (noScreenOpen(client)) {
                    client.setScreenAndShow(new CollectionScreen());
                }
            }

            while (toggleGlowKeyBinding.consumeClick()) {
                TropicalFishConfig config = TropicalFishConfig.get();
                TropicalFishConfig.GlowMode[] modes = TropicalFishConfig.GlowMode.values();
                config.glowMode = modes[(config.glowMode.ordinal() + 1) % modes.length];
                AutoConfig.getConfigHolder(TropicalFishConfig.class).save();

                if (client.player != null) {
                    // Mention the collected-filter when it is active: it culls
                    // glow silently, and "rare mode is broken" is what a
                    // filtered-out reef looks like without this hint.
                    String filter = config.glowOnlyUncollected
                            && config.glowMode != TropicalFishConfig.GlowMode.OFF
                            ? " (only uncollected)" : "";
                    client.player.sendSystemMessage(
                            Component.literal("Tropical fish glow: " + config.glowMode + filter));
                }
            }

            while (toggleNamesKeyBinding.consumeClick()) {
                TropicalFishConfig config = TropicalFishConfig.get();
                config.namesEnabled = !config.namesEnabled;
                AutoConfig.getConfigHolder(TropicalFishConfig.class).save();

                String message = config.namesEnabled ?
                        "Tropical fish names enabled" : "Tropical fish names disabled";

                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal(message));
                }
            }
        });
    }
}
