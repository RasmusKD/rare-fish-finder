package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.gui.CollectionScreen;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A purely cosmetic "Fish Collection" tab in the vanilla advancements screen,
 * drawn after the real tabs with the tropical fish bucket as its icon.
 * Clicking it opens the collection screen, and Esc returns here. Client-side
 * only, works on any server, no datapack and no server advancements involved
 * - the tab geometry replicates AdvancementTabType.ABOVE (28x32, spacing 32,
 * icon offset +6/+9), which is package-private and therefore restated here.
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin extends Screen {

    @Shadow
    @Final
    private Map<AdvancementHolder, AdvancementTab> tabs;

    @Unique
    private static final ItemStack RFF_ICON = new ItemStack(Items.TROPICAL_FISH_BUCKET);
    @Unique
    private static final Identifier RFF_TAB_FIRST = Identifier.withDefaultNamespace("advancements/tab_above_left");
    @Unique
    private static final Identifier RFF_TAB_MIDDLE = Identifier.withDefaultNamespace("advancements/tab_above_middle");

    protected AdvancementsScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private int rff$tabIndex() {
        // ABOVE holds eight tabs; land after the real ones, clamped to the row.
        return Math.min(tabs.size(), 7);
    }

    @Inject(method = "extractWindow", at = @At("TAIL"))
    private void rff$drawFishTab(GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY,
            CallbackInfo ci) {
        int index = rff$tabIndex();
        int x = xo + 32 * index;
        int y = yo - 28;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                index == 0 ? RFF_TAB_FIRST : RFF_TAB_MIDDLE, x, y, 28, 32);
        graphics.fakeItem(RFF_ICON, x + 6, y + 9);
        if (mouseX > x && mouseX < x + 28 && mouseY > y && mouseY < y + 32) {
            graphics.setTooltipForNextFrame(this.font, Component.literal("Fish Collection"), mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rff$clickFishTab(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) {
            return;
        }
        int xo = (this.width - 252) / 2;
        int yo = (this.height - 140) / 2;
        int x = xo + 32 * rff$tabIndex();
        int y = yo - 28;
        if (event.x() > x && event.x() < x + 28 && event.y() > y && event.y() < y + 32) {
            this.minecraft.setScreenAndShow(new CollectionScreen((Screen) (Object) this));
            cir.setReturnValue(true);
        }
    }
}
