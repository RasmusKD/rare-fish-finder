package com.rasmus.rarefishfinder.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.rasmus.rarefishfinder.client.RareFishFinderClient;
import com.rasmus.rarefishfinder.client.Toasts;
import com.rasmus.rarefishfinder.collection.FishCollection;
import com.rasmus.rarefishfinder.gui.CollectionScreen;
import com.rasmus.rarefishfinder.gui.NewCatchToast;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.DyeColor;

/**
 * Headless checks of the catch toasts and of editing the collection, in a
 * fresh singleplayer world. With Fancy Toasts on the classpath
 * (-PfancyToastsTest=...) it also checks what Fancy Toasts shows for our
 * catches. Uses only API that predates the fixes, so the same test runs red
 * on the unfixed code.
 */
public class RareFishFinderClientGameTest implements FabricClientGameTest {

    private static final TropicalFish.Pattern[] PATTERNS = TropicalFish.Pattern.values();
    private static final DyeColor[] COLORS = DyeColor.values();

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitTicks(20);
            List<String> failures = new ArrayList<>();
            run(context, failures, "rareFirstCatchPlaysFanfare", () -> rareFirstCatchPlaysFanfare());
            run(context, failures, "rightClickTogglesACell", () -> rightClickTogglesACell(context));
            run(context, failures, "resetNeedsTwoPresses", () -> resetNeedsTwoPresses(context));
            if (FabricLoader.getInstance().isModLoaded("fancytoasts")) {
                run(context, failures, "fancyToastsShowsRarityAndFanfare",
                        () -> fancyToastsShowsRarityAndFanfare(context));
            }
            if (!failures.isEmpty()) {
                throw new AssertionError(failures.size() + " failed: " + String.join("; ", failures));
            }
        }
    }

    /** Runs every check even when an earlier one fails, so one run shows each failure. */
    private static void run(ClientGameTestContext context, List<String> failures, String name, Runnable test) {
        try {
            test.run();
            System.out.println("[rff-test] PASS " + name);
        } catch (RuntimeException | AssertionError e) {
            System.out.println("[rff-test] FAIL " + name + ": " + e);
            failures.add(name);
            context.runOnClient(client -> client.setScreenAndShow(null));
            context.waitTicks(5);
        }
    }

    private static int rarePacked() {
        // Kob, white body, orange pattern is not one of the 22 named commons.
        int packed = new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.WHITE, DyeColor.ORANGE)
                .getPackedId();
        for (TropicalFish.Variant common : TropicalFish.COMMON_VARIANTS) {
            check(common.getPackedId() != packed, "the chosen rare variant is a common");
        }
        return packed;
    }

    private static void rareFirstCatchPlaysFanfare() {
        int common = TropicalFish.COMMON_VARIANTS.get(0).getPackedId();
        check(new NewCatchToast(rarePacked(), true).getSoundEvent() == SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                "a first rare catch must play the challenge fanfare");
        check(new NewCatchToast(common, false).getSoundEvent() == null,
                "a first common catch must stay quiet");
    }

    private static void openCollection(ClientGameTestContext context) {
        context.runOnClient(client -> client.setScreenAndShow(new CollectionScreen()));
        context.waitForScreen(CollectionScreen.class);
        context.waitTicks(2);
    }

    /** Moves the cursor to a grid cell of the first pattern page (layout of CollectionScreen.init). */
    private static void pointAtCell(ClientGameTestContext context, int row, int col) {
        double[] pos = context.computeOnClient(client -> {
            var window = client.getWindow();
            int panelWidth = 10 + 12 * COLORS.length + 10 + 130 + 10;
            int panelHeight = 10 + 24 + 12 * COLORS.length + 10;
            int gridX = (window.getGuiScaledWidth() - panelWidth) / 2 + 10;
            int gridY = (window.getGuiScaledHeight() - panelHeight) / 2 + 10 + 24;
            double scale = window.getScreenWidth() / (double) window.getGuiScaledWidth();
            return new double[] {(gridX + col * 12 + 6) * scale, (gridY + row * 12 + 6) * scale};
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
        context.waitTicks(1);
    }

    private static void rightClickTogglesACell(ClientGameTestContext context) {
        int row = 1;
        int col = 2;
        int packed = new TropicalFish.Variant(PATTERNS[0], COLORS[row], COLORS[col]).getPackedId();
        openCollection(context);
        check(!context.computeOnClient(client -> FishCollection.isCollected(packed)), "cell starts uncollected");
        pointAtCell(context, row, col);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitTicks(2);
        check(context.computeOnClient(client -> FishCollection.isCollected(packed)),
                "right-click must mark the fish collected");
        context.takeScreenshot("rff-collection-marked");
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        context.waitTicks(2);
        check(!context.computeOnClient(client -> FishCollection.isCollected(packed)),
                "a second right-click must unmark it");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(5);
    }

    private static void resetNeedsTwoPresses(ClientGameTestContext context) {
        context.runOnClient(client -> {
            FishCollection.markCollected(new TropicalFish.Variant(PATTERNS[0], COLORS[0], COLORS[0]).getPackedId());
            FishCollection.markCollected(new TropicalFish.Variant(PATTERNS[1], COLORS[3], COLORS[4]).getPackedId());
            FishCollection.markCollected(new TropicalFish.Variant(PATTERNS[2], COLORS[5], COLORS[6]).getPackedId());
            FishCollection.addCatch(new TropicalFish.Variant(PATTERNS[2], COLORS[5], COLORS[6]).getPackedId());
        });
        check(context.computeOnClient(client -> FishCollection.collectedTotal()) == 3, "three fish set up");
        openCollection(context);
        context.clickScreenButton("Reset");
        context.waitTicks(2);
        check(context.computeOnClient(client -> FishCollection.collectedTotal()) == 3,
                "one Reset press must not wipe the collection");
        context.takeScreenshot("rff-collection-reset-armed");
        context.clickScreenButton("Sure?");
        context.waitTicks(2);
        check(context.computeOnClient(client -> FishCollection.collectedTotal()) == 0,
                "the confirming press must empty the collection");
        check(context.computeOnClient(client -> FishCollection.totalCatches()) == 0,
                "reset must clear the catch counts too");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(5);
    }

    /** The toast the mod shows for a first catch (its private milestoneToast). */
    private static Toast catchToast(int packed, boolean rare) {
        try {
            Method method = RareFishFinderClient.class.getDeclaredMethod("milestoneToast", int.class, boolean.class);
            method.setAccessible(true);
            return (Toast) method.invoke(null, packed, rare);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("milestoneToast not reachable", e);
        }
    }

    private static void fancyToastsShowsRarityAndFanfare(ClientGameTestContext context) {
        FancyToastsProbe.BANNERS.clear();
        FancyToastsProbe.SOUNDS.clear();
        context.runOnClient(client -> Toasts.show(catchToast(rarePacked(), true)));
        context.waitFor(client -> !FancyToastsProbe.BANNERS.isEmpty() && !FancyToastsProbe.SOUNDS.isEmpty(), 400);
        context.waitTicks(50);
        context.takeScreenshot("rff-fancy-rare-catch");
        String banner = FancyToastsProbe.BANNERS.get(0);
        String sound = FancyToastsProbe.SOUNDS.get(0);
        System.out.println("[rff-test] fancy rare banner='" + banner + "' sound=" + sound);
        check(banner.equals("Rare fish collected!"), "Fancy Toasts banner must name the rarity, was '" + banner + "'");
        check(sound.endsWith("ui.toast.challenge_complete"), "a rare catch must play the challenge fanfare, was " + sound);

        int common = TropicalFish.COMMON_VARIANTS.get(0).getPackedId();
        context.runOnClient(client -> Toasts.show(catchToast(common, false)));
        context.waitFor(client -> FancyToastsProbe.BANNERS.size() >= 2, 1200);
        String commonBanner = FancyToastsProbe.BANNERS.get(1);
        System.out.println("[rff-test] fancy common banner='" + commonBanner + "' sound=" + FancyToastsProbe.SOUNDS.get(1));
        check(commonBanner.equals("New fish collected!"),
                "Fancy Toasts banner for a common must say so, was '" + commonBanner + "'");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
