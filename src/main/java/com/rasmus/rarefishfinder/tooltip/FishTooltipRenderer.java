package com.rasmus.rarefishfinder.tooltip;

import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.config.TropicalFishConfig;
import java.lang.reflect.Method;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders the actual fish model inside the bucket's tooltip, posed at a
 * side view so the pattern and colors are visible. Also the shared home of
 * the display-fish factory and the extractor draw call, reused by the
 * collection screen.
 */
public final class FishTooltipRenderer implements ClientTooltipComponent {
    private static final int MODEL_WIDTH = 58;
    private static final int MODEL_HEIGHT = 40;
    private static final int MODEL_SIZE = 52;
    private static final int TEXT_GAP = 2;
    private static final int DISPLAY_FISH_ID = 0x7F00F1;
    private static final Identifier TROPICAL_FISH_TYPE_ID =
            Identifier.fromNamespaceAndPath("minecraft", "tropical_fish");

    // The registry constant holding EntityType.TROPICAL_FISH moved classes
    // between 26.1 and 26.2, and GuiGraphicsExtractor#entity changed its JOML
    // parameter types from Vector3f/Quaternionf to the -fc interfaces. The
    // registry lookup and the by-name method lookup are stable in both, which
    // is what lets one jar run on both versions.
    private static Method entityExtractMethod;
    private static boolean entityExtractMissing;

    // One cached display fish; swapping variant components on it is cheap,
    // creating entities every frame is not. Tied to the level it was made in.
    private static TropicalFish displayFish;
    private static Level displayFishLevel;
    private static int displayFishVariant = -1;

    // NEW-badge state: captured before the hover marks the variant collected,
    // and kept while the same variant stays hovered.
    private static int badgeVariant = -1;
    private static boolean badgeWasNew;

    private final ItemStack bucket;

    public FishTooltipRenderer(FishTooltip tooltip) {
        this.bucket = tooltip.bucket();
    }

    @Override
    public int getWidth(Font font) {
        // The label ("Solid Rare ✦ NEW") can be wider than the model box;
        // report the real width so the tooltip frame grows instead of the
        // text spilling over its edge.
        return Math.max(MODEL_WIDTH, font.width(currentLabel()));
    }

    @Override
    public int getHeight(Font font) {
        return MODEL_HEIGHT + TEXT_GAP + 9;
    }

    @Override
    public void extractText(GuiGraphicsExtractor extractor, Font font, int x, int y) {
        // State only. Drawing happens in extractImage, which receives the
        // tooltip's actual width: other lines (like a long item name) can
        // make the box wider than this component, and centering on our own
        // width alone would leave everything hugging the left edge.
        int packed = packedVariant(withVariantComponents(bucket));

        if (packed != badgeVariant) {
            badgeVariant = packed;
            badgeWasNew = !FishCollection.isCollected(packed);
        }
        if (TropicalFishConfig.get().hoverCollects) {
            FishCollection.markCollected(packed);
        }
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height,
            GuiGraphicsExtractor extractor) {
        TropicalFishConfig config = TropicalFishConfig.get();
        // Left-aligned like the tooltip's text lines, so the component reads
        // as part of the list regardless of how wide the title makes the box.
        extractFish(extractor, packedVariant(withVariantComponents(bucket)),
                x, y, x + MODEL_WIDTH, y + MODEL_HEIGHT, MODEL_SIZE,
                config.tooltipFishYaw, config.tooltipFishTilt);

        Component label = currentLabel();
        extractor.text(font, label.getVisualOrderText(),
                x, y + MODEL_HEIGHT + TEXT_GAP, 0xFFFFFFFF, true);
    }

    /**
     * The badge line under the model. Built identically wherever it is
     * needed (width measurement and drawing), without side effects.
     */
    private Component currentLabel() {
        int packed = packedVariant(withVariantComponents(bucket));
        TropicalFish.Variant variant = new TropicalFish.Variant(packed);
        boolean rare = !TropicalFish.COMMON_VARIANTS.contains(variant);
        boolean solid = variant.baseColor() == variant.patternColor();
        boolean isNew = packed == badgeVariant
                ? badgeWasNew : !FishCollection.isCollected(packed);

        Component label = rare
                ? Component.literal("Rare").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : Component.literal("Common").withStyle(ChatFormatting.GRAY);
        if (solid) {
            // Solids (base and pattern color matching) are no rarer, just a
            // cool find, and they earn a badge of their own.
            label = Component.empty().append(Component.literal("Solid ")
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)).append(label);
        }
        if (isNew && TropicalFishConfig.get().showNewBadge) {
            label = Component.empty().append(label).append(
                    Component.literal(" ✦ NEW").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }
        return label;
    }

    /**
     * Draws the display fish for a packed variant into a rectangle. Shared by
     * the tooltip and the collection screen.
     */
    public static void extractFish(GuiGraphicsExtractor extractor, int packed,
            int x1, int y1, int x2, int y2, int size, float yaw, float tilt) {
        TropicalFish fish = fishFor(packed);
        if (fish == null) {
            return;
        }

        // Face the fish sideways: fish patterns live on the flanks, so a
        // side view shows what the front view hides.
        fish.setYRot(yaw);
        fish.setXRot(0.0F);
        fish.yBodyRot = yaw;
        fish.yHeadRot = yaw;
        fish.yHeadRotO = yaw;

        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(fish);
        if (renderer == null) {
            return;
        }

        EntityRenderState state = renderer.createRenderState(fish, 1.0F);
        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState livingState) {
            // A fish that is not in water renders lying on its side, flopping.
            livingState.isInWater = true;
        }

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX((float) Math.toRadians(tilt));
        pose.mul(camera);
        Vector3f translation = new Vector3f(0.0F, fish.getBbHeight() / 2.0F + 0.03125F, 0.0F);

        Method entityCall = findEntityExtract();
        if (entityCall == null) {
            return;
        }
        try {
            entityCall.invoke(extractor, state, (float) size, translation, pose, camera,
                    x1, y1, x2, y2);
        } catch (ReflectiveOperationException ignored) {
            entityExtractMissing = true;
        }
    }

    private static Method findEntityExtract() {
        if (entityExtractMissing) {
            return null;
        }
        if (entityExtractMethod == null) {
            for (Method method : GuiGraphicsExtractor.class.getMethods()) {
                if (method.getName().equals("entity") && method.getParameterCount() == 9) {
                    entityExtractMethod = method;
                    break;
                }
            }
            if (entityExtractMethod == null) {
                entityExtractMissing = true;
            }
        }
        return entityExtractMethod;
    }

    /**
     * The cached display fish carrying a given packed variant, or null when
     * no level is available.
     */
    public static TropicalFish fishFor(int packed) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        if (displayFish != null && displayFishLevel == level && displayFishVariant == packed) {
            return displayFish;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(TROPICAL_FISH_TYPE_ID);
        Entity created = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (!(created instanceof TropicalFish fish)) {
            return null;
        }

        TropicalFish.Variant variant = new TropicalFish.Variant(packed);
        ItemStack carrier = new ItemStack(Items.TROPICAL_FISH_BUCKET);
        carrier.set(DataComponents.TROPICAL_FISH_PATTERN, variant.pattern());
        carrier.set(DataComponents.TROPICAL_FISH_BASE_COLOR, variant.baseColor());
        carrier.set(DataComponents.TROPICAL_FISH_PATTERN_COLOR, variant.patternColor());

        fish.setId(DISPLAY_FISH_ID);
        fish.applyComponentsFromItemStack(carrier);

        displayFish = fish;
        displayFishLevel = level;
        displayFishVariant = packed;
        return fish;
    }

    /**
     * Buckets from current servers carry typed tropical fish components;
     * older servers (or ViaVersion) still ship the packed int in the legacy
     * bucket NBT. Upgrade the legacy form to components on a copy, so the
     * display fish can be fed through one path.
     */
    /**
     * The packed variant carried by a tropical fish bucket, or -1 when the
     * stack is not a bucket with a variant. Legacy NBT buckets are upgraded
     * the same way as everywhere else.
     */
    public static int bucketVariant(ItemStack stack) {
        if (!stack.is(Items.TROPICAL_FISH_BUCKET)) {
            return -1;
        }
        ItemStack upgraded = withVariantComponents(stack);
        if (!upgraded.has(DataComponents.TROPICAL_FISH_PATTERN)) {
            return -1;
        }
        return packedVariant(upgraded);
    }

    private static ItemStack withVariantComponents(ItemStack bucket) {
        if (bucket.has(DataComponents.TROPICAL_FISH_PATTERN)) {
            return bucket;
        }

        CustomData data = bucket.get(DataComponents.BUCKET_ENTITY_DATA);
        if (data == null) {
            return bucket;
        }

        int packed = data.copyTag().getIntOr("BucketVariantTag", -1);
        if (packed < 0) {
            return bucket;
        }

        TropicalFish.Variant variant = new TropicalFish.Variant(packed);
        ItemStack upgraded = bucket.copy();
        upgraded.set(DataComponents.TROPICAL_FISH_PATTERN, variant.pattern());
        upgraded.set(DataComponents.TROPICAL_FISH_BASE_COLOR, variant.baseColor());
        upgraded.set(DataComponents.TROPICAL_FISH_PATTERN_COLOR, variant.patternColor());
        return upgraded;
    }

    private static int packedVariant(ItemStack stack) {
        TropicalFish.Pattern pattern = stack.get(DataComponents.TROPICAL_FISH_PATTERN);
        if (pattern == null) {
            return TropicalFish.DEFAULT_VARIANT.getPackedId();
        }
        DyeColor base = stack.getOrDefault(
                DataComponents.TROPICAL_FISH_BASE_COLOR, TropicalFish.DEFAULT_VARIANT.baseColor());
        DyeColor patternColor = stack.getOrDefault(
                DataComponents.TROPICAL_FISH_PATTERN_COLOR,
                TropicalFish.DEFAULT_VARIANT.patternColor());
        return new TropicalFish.Variant(pattern, base, patternColor).getPackedId();
    }
}
