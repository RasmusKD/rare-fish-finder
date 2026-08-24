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
 * Fish Collection tab on 26.2, where AdvancementsScreen no longer passes the
 * window origin to any extract method:
 *
 * <pre>
 * extractInside(graphics)
 * extractWindow(graphics, mouseX, mouseY)
 * extractTooltips(graphics, mouseX, mouseY)
 * </pre>
 *
 * The two ints on extractWindow are the MOUSE, not the origin: vanilla
 * forwards extractRenderState's own mouseX/mouseY. Reading them as an origin
 * compiles and loads happily and then draws the tab wherever the cursor is,
 * so the origin is recomputed here from the screen size, exactly as vanilla
 * 26.1 computed it before handing it down.
 *
 * <p>26.1 keeps the origin parameters and gets
 * {@link AdvancementsScreenLegacyMixin}; the plugin applies exactly one of
 * the pair. All behavior lives in {@link FishTabView}.
 */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenModernMixin extends Screen {

    @Shadow
    @Final
    private Map<AdvancementHolder, AdvancementTab> tabs;

    @Unique
    private final FishTabView rff$view = new FishTabView();

    protected AdvancementsScreenModernMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractWindow", at = @At("TAIL"))
    private void rff$drawFishTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        rff$view.drawTab(this.font, this.tabs, graphics,
                FishTabView.originX(this.width), FishTabView.originY(this.height), mouseX, mouseY);
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
    private void rff$suppressInside(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (rff$view.suppressInside(graphics,
                FishTabView.originX(this.width), FishTabView.originY(this.height))) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTooltips", at = @At("HEAD"), cancellable = true)
    private void rff$suppressTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (rff$view.suppressTooltips()) {
            ci.cancel();
        }
    }
}
