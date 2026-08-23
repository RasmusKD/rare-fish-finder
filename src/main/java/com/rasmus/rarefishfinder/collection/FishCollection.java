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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-server record of which tropical fish variants have been collected in a
 * bucket. Variants are the unit: two buckets of the same variant are
 * indistinguishable, so the collection is a set, not a count. Catches ARE
 * countable, because they are events.
 */
public final class FishCollection {
    private static final Logger LOGGER = LoggerFactory.getLogger("rarefishfinder");

    // 12 patterns x 16 base x 16 pattern colors. Black included: it spawns
    // naturally on Java since 1.19.3 (originally excluded by design).
    public static final int TOTAL_VARIANTS = TropicalFish.Pattern.values().length
            * DyeColor.values().length * DyeColor.values().length;

    private static final Map<Integer, Entry> entries = new HashMap<>();
    private static String loadedKey;
    private static Level loadedLevel;

    // Mirror of the collected variant ids, safe to read every frame from the
    // glow hook without touching key computation or disk.
    private static final Set<Integer> collectedFast = new HashSet<>();

    private FishCollection() {
    }

    public static final class Entry {
        public boolean collected;
        public int catches;
    }

    public static int packedOf(TropicalFish fish) {
        return new TropicalFish.Variant(
                fish.getPattern(), fish.getBaseColor(), fish.getPatternColor()).getPackedId();
    }

    public static void markCollected(int packed) {
        Entry entry = entryFor(packed);
        if (!entry.collected) {
            entry.collected = true;
            collectedFast.add(packed);
            save();
        }
    }

    /** Records a catch; true when this variant was not collected before. */
    public static boolean addCatch(int packed) {
        Entry entry = entryFor(packed);
        boolean wasNew = !entry.collected;
        entry.collected = true;
        entry.catches++;
        collectedFast.add(packed);
        save();
        return wasNew;
    }

    /**
     * Canonical collection number, 1..TOTAL_VARIANTS: patterns in declaration
     * order, then base color, then pattern color. The numbering the toast
     * and the collection screen share.
     */
    public static int numberOf(int packed) {
        var variant = new net.minecraft.world.entity.animal.fish.TropicalFish.Variant(packed);
        int colors = net.minecraft.world.item.DyeColor.values().length;
        return variant.pattern().ordinal() * colors * colors
                + variant.baseColor().getId() * colors
                + variant.patternColor().getId() + 1;
    }

    public static boolean isCollected(int packed) {
        Entry entry = peek(packed);
        return entry != null && entry.collected;
    }

    /**
     * Per-frame-safe collected check for the glow hooks: one reference
     * compare on the current level, then a plain set read. The full key
     * computation only reruns when the level object changes (login, world
     * switch, dimension change).
     */
    public static boolean isCollectedFast(int packed) {
        Level level = Minecraft.getInstance().level;
        if (level != loadedLevel) {
            loadedLevel = level;
            ensureLoaded();
        }
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
        var minecraft = Minecraft.getInstance();
        var server = minecraft.getCurrentServer();
        if (server != null && server.ip != null) {
            return sanitize(server.ip);
        }
        // Singleplayer: key by the world's save folder, so every world gets
        // its own collection instead of all sharing one.
        var integrated = minecraft.getSingleplayerServer();
        if (integrated != null) {
            String folder = integrated.getWorldPath(LevelResource.ROOT)
                    .normalize().getFileName().toString();
            return "sp-" + sanitize(folder);
        }
        return "local";
    }

    private static String sanitize(String raw) {
        return raw.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
    }

    private static Path fileFor(String key) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("rarefishfinder").resolve("collection-" + key + ".json");
    }
}
