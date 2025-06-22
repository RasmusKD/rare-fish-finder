package com.rasmus.rarefishfinder.client;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RareFishFinderClient implements ClientModInitializer {
    public static final String MOD_ID = "rarefishfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding toggleGlowKeyBinding;
    private static KeyBinding toggleNamesKeyBinding;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Test MobGlow mod initialized!");

        toggleGlowKeyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.rarefishfinder.toggleGlow",
                        InputUtil.Type.KEYSYM,
                        71, // G key
                        "key.category.rarefishfinder"
                )
        );

        toggleNamesKeyBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.rarefishfinder.toggleNames",
                        InputUtil.Type.KEYSYM,
                        78, // N key
                        "key.category.rarefishfinder"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleGlowKeyBinding.wasPressed()) {
                TropicalFishConfig config = TropicalFishConfig.get();
                config.glowEnabled = !config.glowEnabled;
                AutoConfig.getConfigHolder(TropicalFishConfig.class).save();

                String message = config.glowEnabled ?
                        "Tropical fish glow enabled" : "Tropical fish glow disabled";

                if (client.player != null) {
                    client.player.sendMessage(Text.literal(message), false);
                }
            }

            while (toggleNamesKeyBinding.wasPressed()) {
                TropicalFishConfig config = TropicalFishConfig.get();
                config.namesEnabled = !config.namesEnabled;
                AutoConfig.getConfigHolder(TropicalFishConfig.class).save();

                String message = config.namesEnabled ?
                        "Tropical fish names enabled" : "Tropical fish names disabled";

                if (client.player != null) {
                    client.player.sendMessage(Text.literal(message), false);
                }
            }
        });
    }
}