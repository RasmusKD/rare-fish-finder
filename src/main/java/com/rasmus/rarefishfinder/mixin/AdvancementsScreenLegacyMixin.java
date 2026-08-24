package com.rasmus.rarefishfinder.mixin;

import com.rasmus.rarefishfinder.gui.FishTabView;
import java.util.Map;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fish Collection tab on 26.1, where AdvancementsScreen hands the window
 * origin down to every extract method:
 *
 * <pre>
 * extractInside(graphics, xo, yo)
 * extractWindow(graphics, xo, yo, mouseX, mouseY)
 * extractTooltips(graphics, mouseX, mouseY, xo, yo)
 * </pre>
 *
 * 26.2 drops the origin from all three and reorders what is left, so it
 * gets its own mixin ({@link AdvancementsScreenModernMixin}) and the plugin
 * applies exactly one of the pair. All behavior lives in {@link FishTabView};
 * these two classes only resolve the coordinates.
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenLegacyMixin extends Screen {

    @Shadow
    @Final
    private Map<AdvancementHolder, AdvancementTab> tabs;

    @Unique
    private final FishTabView rff$view = new FishTabView();

    protected AdvancementsScreenLegacyMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractWindow", at = @At("TAIL"))
    private void rff$drawFishTab(GuiGraphicsExtractor graphics, int xo, int yo, int mouseX, int mouseY,
            CallbackInfo ci) {
        rff$view.drawTab(this.font, this.tabs, graphics, xo, yo, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rff$clickFishTab(MouseButtonEvent event, boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (rff$view.click((Screen) (Object) this, this.minecraft, this.tabs,
                this.width, this.height, event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractInside", at = @At("HEAD"), cancellable = true)
    private void rff$suppressInside(GuiGraphicsExtractor graphics, int xo, int yo, CallbackInfo ci) {
        if (rff$view.suppressInside(graphics, xo, yo)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTooltips", at = @At("HEAD"), cancellable = true)
    private void rff$suppressTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int xo, int yo,
            CallbackInfo ci) {
        if (rff$view.suppressTooltips()) {
            ci.cancel();
        }
    }
}
