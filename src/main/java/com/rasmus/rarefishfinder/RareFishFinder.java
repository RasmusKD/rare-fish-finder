package com.rasmus.rarefishfinder;

import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import net.fabricmc.api.ModInitializer;

public class RareFishFinder implements ModInitializer {
    @Override
    public void onInitialize() {
        TropicalFishConfig.register();
    }
}