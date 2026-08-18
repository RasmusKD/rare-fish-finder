package com.rasmus.rarefishfinder.collection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-server record of which tropical fish variants have been spotted
 * swimming and which have been collected in a bucket. Variants are the unit:
 * two buckets of the same variant are indistinguishable, so the collection is
 * a set, not a count. Catches ARE countable, because they are events.
 */
public final class FishCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger("rarefishfinder");

    // 12 patterns x 16 base x 16 pattern colors. Black included: it spawns
    // naturally on Java since 1.19.3 (originally excluded by design).
    public static final int TOTAL_VARIANTS = TropicalFish.Pattern.values().length
            * DyeColor.values().length * DyeColor.values().length;

    private static final Map<Integer, Entry> entries = new HashMap<>();
    private static String loadedKey;

    // Fast path for the per-tick spotted hook: variants already handled this
    // session skip the key check and map lookup entirely. Cleared when the
    // loaded collection swaps to another server.
    private static final Set<Integer> spottedFast = new HashSet<>();

    // Mirror of the collected variant ids, safe to read every frame from the
    // glow hook without touching key computation or disk.
    private static final Set<Integer> collectedFast = new HashSet<>();

    private FishCollection() {
    }

    public static final class Entry {
        public boolean spotted;
        public boolean collected;
        public int catches;
    }

    public static int packedOf(TropicalFish fish) {
        return new TropicalFish.Variant(
                fish.getPattern(), fish.getBaseColor(), fish.getPatternColor()).getPackedId();
    }

    public static void markSpotted(int packed) {
        if (!spottedFast.add(packed)) {
            return;
        }
        Entry entry = entryFor(packed);
        if (!entry.spotted) {
            entry.spotted = true;
            save();
        }
    }

    public static void markCollected(int packed) {
        Entry entry = entryFor(packed);
        if (!entry.collected) {
            entry.collected = true;
            entry.spotted = true;
            collectedFast.add(packed);
            save();
        }
    }

    public static void addCatch(int packed) {
        Entry entry = entryFor(packed);
        entry.collected = true;
        entry.spotted = true;
        entry.catches++;
        collectedFast.add(packed);
        save();
    }

    public static boolean isSpotted(int packed) {
        Entry entry = peek(packed);
        return entry != null && entry.spotted;
    }

    public static boolean isCollected(int packed) {
        Entry entry = peek(packed);
        return entry != null && entry.collected;
    }

    /**
     * Per-frame-safe collected check: a plain set read, no load or key
     * computation. Populated once the collection loads, which the per-tick
     * spotted hook triggers as soon as any tropical fish is near.
     */
    public static boolean isCollectedFast(int packed) {
        return collectedFast.contains(packed);
    }

    public static int catches(int packed) {
        Entry entry = peek(packed);
        return entry == null ? 0 : entry.catches;
    }

    public static int collectedTotal() {
        ensureLoaded();
        return (int) entries.values().stream().filter(e -> e.collected).count();
    }

    public static int collectedCommons() {
        ensureLoaded();
        int count = 0;
        for (TropicalFish.Variant variant : TropicalFish.COMMON_VARIANTS) {
            Entry entry = entries.get(variant.getPackedId());
            if (entry != null && entry.collected) {
                count++;
            }
        }
        return count;
    }

    public static int collectedSolids() {
        ensureLoaded();
        int count = 0;
        for (Map.Entry<Integer, Entry> e : entries.entrySet()) {
            if (!e.getValue().collected) {
                continue;
            }
            TropicalFish.Variant variant = new TropicalFish.Variant(e.getKey());
            if (variant.baseColor() == variant.patternColor()) {
                count++;
            }
        }
        return count;
    }

    public static int totalCatches() {
        ensureLoaded();
        return entries.values().stream().mapToInt(e -> e.catches).sum();
    }

    // ---------------------------------------------------------------- storage

    private static Entry entryFor(int packed) {
        ensureLoaded();
        return entries.computeIfAbsent(packed, k -> new Entry());
    }

    private static Entry peek(int packed) {
        ensureLoaded();
        return entries.get(packed);
    }

    /**
     * Collections are meaningful per server, so the file is keyed by the
     * server address. Switching servers swaps the loaded set transparently.
     */
    private static void ensureLoaded() {
        String key = currentKey();
        if (key.equals(loadedKey)) {
            return;
        }
        entries.clear();
        spottedFast.clear();
        collectedFast.clear();
        loadedKey = key;
        Path file = fileFor(key);
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                Entry entry = new Entry();
                entry.spotted = o.has("spotted") && o.get("spotted").getAsBoolean();
                entry.collected = o.has("collected") && o.get("collected").getAsBoolean();
                entry.catches = o.has("catches") ? o.get("catches").getAsInt() : 0;
                int packedKey = Integer.parseInt(e.getKey());
                entries.put(packedKey, entry);
                if (entry.collected) {
                    collectedFast.add(packedKey);
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read fish collection {}", file, e);
        }
    }

    private static void save() {
        if (loadedKey == null) {
            return;
        }
        JsonObject root = new JsonObject();
        for (Map.Entry<Integer, Entry> e : entries.entrySet()) {
            JsonObject o = new JsonObject();
            o.addProperty("spotted", e.getValue().spotted);
            o.addProperty("collected", e.getValue().collected);
            o.addProperty("catches", e.getValue().catches);
            root.add(String.valueOf(e.getKey()), o);
        }
        Path file = fileFor(loadedKey);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, root.toString());
        } catch (IOException e) {
            LOGGER.warn("Could not save fish collection {}", file, e);
        }
    }

    private static String currentKey() {
        var server = Minecraft.getInstance().getCurrentServer();
        if (server == null || server.ip == null) {
            return "local";
        }
        return server.ip.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    private static Path fileFor(String key) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("rarefishfinder").resolve("collection-" + key + ".json");
    }
}
