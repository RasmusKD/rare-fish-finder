package com.rasmus.rarefishfinder.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
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

    private static final KeyMapping.Category RAREFISH_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("rarefishfinder", "category"));

    @Override
    public void onInitializeClient() {
        LOGGER.info("Rare Fish Finder initialized!");

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleGlowKeyBinding.consumeClick()) {
                TropicalFishConfig config = TropicalFishConfig.get();
                config.glowEnabled = !config.glowEnabled;
                AutoConfig.getConfigHolder(TropicalFishConfig.class).save();

                String message = config.glowEnabled ?
                        "Tropical fish glow enabled" : "Tropical fish glow disabled";

                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal(message));
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
